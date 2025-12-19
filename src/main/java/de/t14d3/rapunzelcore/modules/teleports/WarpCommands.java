package de.t14d3.rapunzelcore.modules.teleports;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.entities.Warp;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpCommands {
    private final Main plugin;

    public WarpCommands(Main plugin) {
        this.plugin = plugin;
    }

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
                    player.teleport(warp.getLocation());
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.warp.success", warpName));
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
                    WarpsRepository.setWarp(warpName, player.getLocation(), permission);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.setwarp.success", warpName).color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
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
                    WarpsRepository.deleteWarp(warpName);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.delwarp.success", warpName));
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
                        Location loc = warp.getLocation();
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
                    WarpsRepository.setSpawn(player.getWorld().getName(), player.getLocation());
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.setspawn.success").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }
}
