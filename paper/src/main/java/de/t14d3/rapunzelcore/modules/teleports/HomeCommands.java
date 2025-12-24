package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.modules.commands.Command;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class HomeCommands implements Command {
    private final RapunzelPaperCore plugin;

    public HomeCommands(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        register();
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("rapunzelcore.homes.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("rapunzelcore.homes." + i)) {
                return i;
            }
        }
        return 1; // Default to 1 home if no specific permission is found
    }

    public void register() {
        // Home command
        new CommandAPICommand("home")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Teleports the player to a home location.")
                .withPermission("rapunzelcore.home")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    UUID playerId = player.getUniqueId();
                    String requestedHome = homeName;

                    HomesRepository.getHomeAsync(playerId, requestedHome).whenComplete((home, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player online = Bukkit.getPlayer(playerId);
                        if (online == null || !online.isOnline()) return;

                        if (error != null) {
                            plugin.getLogger().warning("Failed to load home '" + requestedHome + "' for " + playerId + ": " + error.getMessage());
                            return;
                        }

                        if (home == null) {
                            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.home.error.no_home", requestedHome));
                            return;
                        }

                        if (TeleportsNetwork.isLocal(home.server())) {
                            org.bukkit.World world = Bukkit.getWorld(home.world());
                            if (world == null) {
                                online.sendMessage(plugin.getMessageHandler().getMessage("teleports.home.error.no_home", requestedHome));
                                return;
                            }
                            Location homeLocation = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
                            online.teleport(homeLocation);
                            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.home.success", requestedHome));
                            return;
                        }

                        queueHomeTeleport(playerId, home);
                    }));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Sethome command
        new CommandAPICommand("sethome")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Sets the player's home location.")
                 .withPermission("rapunzelcore.sethome")
                 .executes((executor, args) -> {
                     Player player = (Player) executor;
                    String homeNameArg = (String) args.get("homeName");
                    final String homeName = (homeNameArg == null) ? "default" : homeNameArg;

                    UUID playerId = player.getUniqueId();
                    int maxHomes = getMaxHomes(player);
                    Location location = player.getLocation();
                    String worldName = location.getWorld() != null ? location.getWorld().getName() : null;
                    String serverName = TeleportsNetwork.localServerNameIfKnown();
                    HomesRepository.HomeSnapshot snapshot = new HomesRepository.HomeSnapshot(
                        homeName,
                        worldName,
                        serverName,
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch()
                    );

                    HomesRepository.setHomeAsync(playerId, snapshot, maxHomes).whenComplete((result, error) ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player online = Bukkit.getPlayer(playerId);
                            if (online == null || !online.isOnline()) return;

                            if (error != null || result == null || result.status() == null) {
                                String msg = error != null ? error.getMessage() : "unknown";
                                plugin.getLogger().warning("Failed to set home '" + homeName + "' for " + playerId + ": " + msg);
                                return;
                            }

                            if (result.status() == HomesRepository.SetHomeStatus.LIMIT_REACHED) {
                                online.sendMessage(plugin.getMessageHandler()
                                    .getMessage("teleports.sethome.error.limit_reached", String.valueOf(result.maxHomes()))
                                    .color(NamedTextColor.RED));
                                return;
                            }

                            if (result.status() == HomesRepository.SetHomeStatus.UPDATED) {
                                online.sendMessage(plugin.getMessageHandler()
                                    .getMessage("teleports.sethome.updated", homeName)
                                    .color(NamedTextColor.YELLOW));
                                return;
                            }

                            online.sendMessage(plugin.getMessageHandler()
                                .getMessage("teleports.sethome.success", homeName)
                                .color(NamedTextColor.GREEN));

                            if (!online.hasPermission("rapunzelcore.homes.unlimited")) {
                                int remaining = result.remainingHomes();
                                if (remaining <= 0) {
                                    online.sendMessage(plugin.getMessageHandler()
                                        .getMessage("teleports.sethome.limit_reached")
                                        .color(NamedTextColor.GOLD));
                                } else {
                                    online.sendMessage(plugin.getMessageHandler()
                                        .getMessage("teleports.sethome.remaining", String.valueOf(remaining))
                                        .color(NamedTextColor.GRAY));
                                }
                            }
                        })
                    );

                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Delhome command
        new CommandAPICommand("delhome")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Deletes the player's home location.")
                .withPermission("rapunzelcore.delhome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    UUID playerId = player.getUniqueId();
                    String targetHome = homeName;

                    HomesRepository.deleteHomeAsync(playerId, targetHome).whenComplete((deleted, error) ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player online = Bukkit.getPlayer(playerId);
                            if (online == null || !online.isOnline()) return;

                            if (error != null) {
                                plugin.getLogger().warning("Failed to delete home '" + targetHome + "' for " + playerId + ": " + error.getMessage());
                                return;
                            }

                            if (deleted == null || !deleted) {
                                online.sendMessage(plugin.getMessageHandler().getMessage("teleports.delhome.error.no_home", targetHome));
                                return;
                            }

                            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.delhome.success", targetHome));
                        })
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Homes command
        new CommandAPICommand("homes")
                .withFullDescription("Lists all your home locations.")
                .withPermission("rapunzelcore.homes")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    UUID playerId = player.getUniqueId();

                    HomesRepository.getHomesAsync(playerId).whenComplete((homes, error) ->
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player online = Bukkit.getPlayer(playerId);
                            if (online == null || !online.isOnline()) return;

                            if (error != null) {
                                plugin.getLogger().warning("Failed to list homes for " + playerId + ": " + error.getMessage());
                                return;
                            }

                            if (homes == null || homes.isEmpty()) {
                                online.sendMessage(plugin.getMessageHandler().getMessage("teleports.homes.error.none"));
                                return;
                            }

                            Component message = plugin.getMessageHandler().getMessage("teleports.homes.header");
                            for (HomesRepository.HomeSnapshot home : homes) {
                                if (home == null || home.name() == null) continue;

                                if (!TeleportsNetwork.isLocal(home.server())) {
                                    message = message.appendNewline().append(Component.text("- " + home.name() + " (" + home.server() + ")"));
                                    continue;
                                }

                                int bx = (int) Math.floor(home.x());
                                int by = (int) Math.floor(home.y());
                                int bz = (int) Math.floor(home.z());
                                message = message.appendNewline().append(plugin.getMessageHandler().getMessage("teleports.homes.entry",
                                    home.name(),
                                    String.valueOf(bx),
                                    String.valueOf(by),
                                    String.valueOf(bz)
                                ));
                            }
                            online.sendMessage(message);
                        })
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    @Override
    public void unregister() {
        List.of("home", "sethome", "delhome", "homes").forEach(CommandAPI::unregister);
    }

    private void queueHomeTeleport(UUID playerId, HomesRepository.HomeSnapshot home) {
        if (playerId == null || home == null) return;
        String targetServer = home.server();
        if (targetServer == null || targetServer.isBlank()) return;

        String playerUuid = playerId.toString();
        CoreDatabase.runAsync(() -> PendingTeleportsRepository.create(
            playerUuid,
            targetServer,
            TeleportsNetwork.TeleportsActions.HOME,
            home.name()
        )).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) return;
            if (error != null) {
                plugin.getLogger().warning("Failed to queue home teleport for " + playerId + ": " + error.getMessage());
                return;
            }

            new de.t14d3.rapunzellib.network.NetworkEventBus(plugin.getMessenger()).sendToProxy(
                de.t14d3.rapunzelcore.network.NetworkChannels.TELEPORTS_PROXY,
                new de.t14d3.rapunzelcore.modules.teleports.network.ProxyConnectRequest(
                    playerUuid,
                    targetServer
                )
            );

            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.home.success", home.name()));
        }));
    }
}
