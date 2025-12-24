package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.modules.teleports.network.NotifyPlayerMessage;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeleportsModule implements Module, Listener, AutoCloseable {
    private boolean enabled = false;
    private RapunzelCore core;
    private YamlConfig config;
    private RapunzelPaperCore plugin;
    private NetworkEventBus bus;
    private NetworkEventBus.Subscription notifySub;

    private Command warpCommands;
    private Command tpaCommands;
    private Command homeCommands;

    private record PendingTeleportWork(String action, String arg) {
    }

    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        this.core = core;
        this.config = loadConfig();
        enabled = true;

        if (environment == Environment.PAPER) {
            this.plugin = (RapunzelPaperCore) core;
            this.bus = new NetworkEventBus(plugin.getMessenger());

            this.notifySub = bus.register(
                de.t14d3.rapunzelcore.network.NetworkChannels.TELEPORTS_BACKEND,
                NotifyPlayerMessage.class,
                (payload, source) -> handleNotify(payload)
            );

            Bukkit.getPluginManager().registerEvents(this, plugin);

            this.warpCommands = new WarpCommands(plugin);
            this.tpaCommands = new TpaCommands(plugin);
            this.homeCommands = new HomeCommands(plugin);
        }
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        // Commands are automatically unregistered
        if (environment == Environment.PAPER) {
            warpCommands.unregister();
            tpaCommands.unregister();
            homeCommands.unregister();
            close();
        }
    }

    @Override
    public String getName() {
        return "teleports";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.home", "op"),
                Map.entry("rapunzelcore.sethome", "op"),
                Map.entry("rapunzelcore.delhome", "op"),
                Map.entry("rapunzelcore.homes", "op"),
                Map.entry("rapunzelcore.homes.unlimited", "op"),
                Map.entry("rapunzelcore.homes.*", "op"),
                Map.entry("rapunzelcore.warp", "op"),
                Map.entry("rapunzelcore.setwarp", "op"),
                Map.entry("rapunzelcore.delwarp", "op"),
                Map.entry("rapunzelcore.warps", "op"),
                Map.entry("rapunzelcore.spawn", "op"),
                Map.entry("rapunzelcore.setspawn", "op"),
                Map.entry("rapunzelcore.tpa", "op"),
                Map.entry("rapunzelcore.tpahere", "op"),
                Map.entry("rapunzelcore.tpaccept", "op"),
                Map.entry("rapunzelcore.tpdeny", "op"),
                Map.entry("rapunzelcore.tptoggle", "op"),
                Map.entry("rapunzelcore.tphere", "op"),
                Map.entry("rapunzelcore.tpo", "op"),
                Map.entry("rapunzelcore.tpohere", "op")
        );
    }
    public TeleportsModule() {

    }

    private void handleNotify(NotifyPlayerMessage payload) {
        if (payload == null || payload.playerUuid() == null || payload.messageKey() == null) return;
        UUID id;
        try {
            id = UUID.fromString(payload.playerUuid());
        } catch (Exception ignored) {
            return;
        }

        Player player = Bukkit.getPlayer(id);
        if (player == null) return;
        player.sendMessage(plugin.getMessageHandler().getMessage(payload.messageKey(), payload.args()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerRepository.refreshFromDb(id);
            HomesRepository.refreshFromDb(id.toString());

            String local = TeleportsNetwork.localServerNameIfKnown();
            if (local == null || local.isBlank()) {
                local = TeleportsNetwork.resolveLocalServerName().join();
            }
            if (local == null || local.isBlank()) return;

            final String localServer = local;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player player = Bukkit.getPlayer(id);
                if (player == null || !player.isOnline()) return;
                applyPending(player, localServer);
            }, 2L);
        });
    }

    private void applyPending(Player player, String localServer) {
        if (player == null || localServer == null || localServer.isBlank()) return;
        UUID playerId = player.getUniqueId();
        String playerUuid = playerId.toString();
        String targetServer = localServer.trim();

        CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            List<de.t14d3.rapunzelcore.database.entities.PendingTeleport> pendingList =
                PendingTeleportsRepository.getInstance().findBy("playerUuid", playerUuid).stream()
                    .sorted(Comparator.comparingLong(de.t14d3.rapunzelcore.database.entities.PendingTeleport::getCreatedAt).reversed())
                    .toList();

            for (de.t14d3.rapunzelcore.database.entities.PendingTeleport pending : pendingList) {
                if (pending == null) continue;
                String pendingTarget = pending.getTargetServer();
                if (pendingTarget == null || !targetServer.equalsIgnoreCase(pendingTarget)) continue;

                PendingTeleportsRepository.getInstance().deleteById(pending.getId());
                CoreDatabase.getEntityManager().flush();
                return new PendingTeleportWork(pending.getAction(), pending.getArg());
            }
            return null;
        })).whenComplete((work, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) return;

            if (error != null) {
                plugin.getLogger().warning("Failed to load pending teleports for " + playerId + ": " + error.getMessage());
                return;
            }
            if (work == null) return;

            try {
                executePending(online, work);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute pending teleport: " + e.getMessage());
            }
        }));
    }

    private void executePending(Player player, PendingTeleportWork pending) {
        String action = pending.action();
        String arg = pending.arg();

        if (TeleportsNetwork.TeleportsActions.HOME.equals(action)) {
            if (arg == null || arg.isBlank()) return;
            UUID playerId = player.getUniqueId();
            String homeName = arg;

            HomesRepository.getHomeAsync(playerId, homeName).whenComplete((home, error) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player online = Bukkit.getPlayer(playerId);
                    if (online == null || !online.isOnline()) return;

                    if (error != null) {
                        plugin.getLogger().warning("Failed to load pending home '" + homeName + "' for " + playerId + ": " + error.getMessage());
                        return;
                    }
                    teleportNow(online, home);
                })
            );
            return;
        }

        if (TeleportsNetwork.TeleportsActions.WARP.equals(action)) {
            de.t14d3.rapunzelcore.database.entities.Warp warp = WarpsRepository.getWarp(arg);
            teleportNow(player, warp);
            return;
        }

        if (TeleportsNetwork.TeleportsActions.TPA_TO_PLAYER.equals(action)) {
            if (arg == null || arg.isBlank()) return;
            UUID targetId;
            try {
                targetId = UUID.fromString(arg);
            } catch (Exception ignored) {
                return;
            }

            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) {
                player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.offline"));
                return;
            }

            player.teleport(target.getLocation());
            player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tphere.success", target.getName()));
        }
    }

    private void teleportNow(Player player, de.t14d3.rapunzelcore.database.entities.Home home) {
        if (home == null) return;
        if (!TeleportsNetwork.isLocal(home.getServer())) return;
        org.bukkit.World world = Bukkit.getWorld(home.getWorld());
        if (world == null) return;
        org.bukkit.Location location = new org.bukkit.Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
        player.teleport(location);
    }

    private void teleportNow(Player player, HomesRepository.HomeSnapshot home) {
        if (home == null) return;
        if (!TeleportsNetwork.isLocal(home.server())) return;
        org.bukkit.World world = Bukkit.getWorld(home.world());
        if (world == null) return;
        org.bukkit.Location location = new org.bukkit.Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        player.teleport(location);
    }

    private void teleportNow(Player player, de.t14d3.rapunzelcore.database.entities.Warp warp) {
        if (warp == null) return;
        if (!TeleportsNetwork.isLocal(warp.getServer())) return;
        org.bukkit.World world = Bukkit.getWorld(warp.getWorld());
        if (world == null) return;
        org.bukkit.Location location = new org.bukkit.Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
        player.teleport(location);
    }

    @Override
    public void close() {
        if (notifySub != null) notifySub.close();
        notifySub = null;
        bus = null;
        plugin = null;
        HandlerList.unregisterAll(this);
    }
}
