package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Warp;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.util.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class WarpCommands implements Command {
    private final RapunzelPaperCore plugin;

    public WarpCommands(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public void register() {
        // Warp command
        new CommandAPICommand("warp")
                .withArguments(new StringArgument("warpName")
                        .replaceSuggestions((info, builder) -> {
                            List<Warp> warps = WarpsRepository.getPublicWarps();
                            for (Warp warp : warps) {
                                String perm = warp.getPermission();
                                if (perm == null || info.sender().hasPermission(perm)) {
                                    builder.suggest(warp.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Teleports to the specified warp.")
                .withPermission("rapunzelcore.warp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    Warp warp = WarpsRepository.getWarp(warpName);
                    if (warp == null) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.warp.error.invalid", warpName));
                        return Command.SINGLE_SUCCESS;
                    }
                    String perm = warp.getPermission();
                    if (perm != null && !player.hasPermission(perm)) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.warp.error.no_permission", warpName));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (TeleportsNetwork.isLocal(warp.getServer())) {
                        player.teleport(Utils.getLocation(warp));
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.warp.success", warpName));
                        return Command.SINGLE_SUCCESS;
                    }

                    queueWarpTeleport(player, warp);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Setwarp command
        new CommandAPICommand("setwarp")
                .withArguments(
                        new StringArgument("warpName"),
                        new StringArgument("permission").setOptional(true)
                )
                .withFullDescription("Sets a warp at your current location. Optionally specify a permission.")
                .withPermission("rapunzelcore.setwarp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    String permission = (String) args.get("permission");        
                    UUID playerId = player.getUniqueId();
                    Location location = player.getLocation();
                    String worldName = location.getWorld() != null ? location.getWorld().getName() : null;
                    String serverName = TeleportsNetwork.localServerNameIfKnown();

                    WarpsRepository.setWarpAsync(
                        warpName,
                        worldName,
                        serverName,
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch(),
                        permission
                    ).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player online = Bukkit.getPlayer(playerId);
                        if (online == null || !online.isOnline()) return;
                        if (error != null) {
                            plugin.getLogger().warning("Failed to set warp '" + warpName + "' for " + playerId + ": " + error.getMessage());
                            return;
                        }
                        online.sendMessage(plugin.getMessageHandler().getMessage("teleports.setwarp.success", warpName).color(NamedTextColor.GREEN));
                    }));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Delwarp command
        new CommandAPICommand("delwarp")
                .withArguments(new StringArgument("warpName"))
                .withFullDescription("Deletes the specified warp.")
                .withPermission("rapunzelcore.delwarp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    Location location = WarpsRepository.getWarpLocation(warpName);
                    if (location == null) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.delwarp.error.invalid", warpName));
                        return Command.SINGLE_SUCCESS;
                    }
                    UUID playerId = player.getUniqueId();
                    WarpsRepository.deleteWarpAsync(warpName).whenComplete((deleted, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player online = Bukkit.getPlayer(playerId);
                        if (online == null || !online.isOnline()) return;
                        if (error != null) {
                            plugin.getLogger().warning("Failed to delete warp '" + warpName + "' for " + playerId + ": " + error.getMessage());
                            return;
                        }
                        if (deleted == null || !deleted) {
                            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.delwarp.error.invalid", warpName));
                            return;
                        }
                        online.sendMessage(plugin.getMessageHandler().getMessage("teleports.delwarp.success", warpName));
                    }));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Warps command
        new CommandAPICommand("warps")
                .withFullDescription("Lists all available warps.")
                .withPermission("rapunzelcore.warps")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    List<Warp> warps = WarpsRepository.getWarps();
                    if (warps.isEmpty()) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.warps.error.none"));
                        return Command.SINGLE_SUCCESS;
                    }
                    Component message = plugin.getMessageHandler().getMessage("teleports.warps.header");
                    for (Warp warp : warps) {
                        String perm = warp.getPermission();
                        if (!TeleportsNetwork.isLocal(warp.getServer())) {
                            message = message.appendNewline().append(Component.text("- " + warp.getName() + " (" + warp.getServer() + ")"));
                            continue;
                        }
                        Location loc = Utils.getLocation(warp);
                        Component entry = plugin.getMessageHandler().getMessage("teleports.warps.entry",
                                warp.getName(),
                                String.valueOf(loc.getBlockX()),
                                String.valueOf(loc.getBlockY()),
                                String.valueOf(loc.getBlockZ())
                        );
                        if (perm != null) {
                            entry = entry.append(plugin.getMessageHandler().getMessage("teleports.warps.permission", perm));
                        }
                        message = message.appendNewline().append(entry);
                    }
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Spawn command
        new CommandAPICommand("spawn")
                .withFullDescription("Teleports the player to the world's spawn location.")
                .withPermission("rapunzelcore.spawn")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location spawnLocation = WarpsRepository.getSpawn(player.getWorld().getName());
                    if (spawnLocation == null) {
                        spawnLocation = player.getWorld().getSpawnLocation();
                    }
                    player.teleport(spawnLocation);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.spawn.success"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Setspawn command
        new CommandAPICommand("setspawn")
                .withFullDescription("Sets the spawn location for the current world.")
                .withPermission("rapunzelcore.setspawn")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    UUID playerId = player.getUniqueId();
                    Location location = player.getLocation();
                    String worldName = location.getWorld() != null ? location.getWorld().getName() : null;
                    String spawnName = "__internal__" + worldName + "__spawn__";
                    String serverName = TeleportsNetwork.localServerNameIfKnown();

                    WarpsRepository.setWarpAsync(
                        spawnName,
                        worldName,
                        serverName,
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch(),
                        null
                    ).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                        Player online = Bukkit.getPlayer(playerId);
                        if (online == null || !online.isOnline()) return;
                        if (error != null) {
                            plugin.getLogger().warning("Failed to set spawn for " + playerId + ": " + error.getMessage());
                            return;
                        }
                        online.sendMessage(plugin.getMessageHandler().getMessage("teleports.setspawn.success").color(NamedTextColor.GREEN));
                    }));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    private void queueWarpTeleport(Player player, Warp warp) {
        if (player == null || warp == null) return;
        String targetServer = warp.getServer();
        if (targetServer == null || targetServer.isBlank()) return;

        UUID playerId = player.getUniqueId();
        String playerUuid = playerId.toString();
        String warpName = warp.getName();

        CoreDatabase.runAsync(() -> PendingTeleportsRepository.create(
            playerUuid,
            targetServer,
            TeleportsNetwork.TeleportsActions.WARP,
            warpName
        )).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) return;
            if (error != null) {
                plugin.getLogger().warning("Failed to queue warp teleport for " + playerId + ": " + error.getMessage());
                return;
            }

            new de.t14d3.rapunzellib.network.NetworkEventBus(plugin.getMessenger()).sendToProxy(
                de.t14d3.rapunzelcore.network.NetworkChannels.TELEPORTS_PROXY,
                new de.t14d3.rapunzelcore.modules.teleports.network.ProxyConnectRequest(
                    playerUuid,
                    targetServer
                )
            );

            online.sendMessage(plugin.getMessageHandler().getMessage("teleports.warp.success", warpName));
        }));
    }
}
