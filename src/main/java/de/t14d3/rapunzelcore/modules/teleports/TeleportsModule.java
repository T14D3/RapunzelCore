package de.t14d3.rapunzelcore.modules.teleports;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.entities.Home;
import de.t14d3.rapunzelcore.entities.Warp;
import de.t14d3.rapunzelcore.modules.Module;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class TeleportsModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private CoreDatabase coreDatabase;
    private FileConfiguration config;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        this.coreDatabase = plugin.getCoreDatabase();
        this.config = loadConfig(plugin);
        enabled = true;

        registerCommands();
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        // Commands are automatically unregistered
        enabled = false;
    }

    @Override
    public String getName() {
        return "teleports";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void registerCommands() {
        // Home commands
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
                    player.sendMessage(String.valueOf(HomesRepository.getInstance().findAll().size()));
                    Location homeLocation = HomesRepository.getHomeLocation(player, homeName);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("teleports.home.error.no_home", homeName));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(homeLocation);
                    player.sendMessage(plugin.getMessage("teleports.home.success", homeName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("sethome")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Sets the player's home location.")
                .withPermission("rapunzelcore.sethome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    HomesRepository.setHome(player, homeName, player.getLocation());
                    player.sendMessage(plugin.getMessage("teleports.sethome.success", homeName).color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

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
                    Location homeLocation = HomesRepository.getHomeLocation(player, homeName);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("teleports.delhome.error.no_home", homeName));
                        return Command.SINGLE_SUCCESS;
                    }
                    HomesRepository.deleteHome(player, homeName);
                    player.sendMessage(plugin.getMessage("teleports.delhome.success", homeName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("homes")
                .withFullDescription("Lists all your home locations.")
                .withPermission("rapunzelcore.homes")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    List<Home> homes = HomesRepository.getHomes(player);
                    if (homes.isEmpty()) {
                        player.sendMessage(plugin.getMessage("teleports.homes.error.none"));
                        return Command.SINGLE_SUCCESS;
                    }
                    Component message = plugin.getMessage("teleports.homes.header");
                    for (Home home : homes) {
                        Location loc = home.getLocation();
                        message = message.appendNewline().append(plugin.getMessage("teleports.homes.entry",
                                home.getName(),
                                String.valueOf(loc.getBlockX()),
                                String.valueOf(loc.getBlockY()),
                                String.valueOf(loc.getBlockZ())
                        ));
                    }
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Warp commands
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
                        player.sendMessage(plugin.getMessage("teleports.warp.error.invalid", warpName));
                        return Command.SINGLE_SUCCESS;
                    }
                    String perm = warp.getPermission();
                    if (perm != null && !player.hasPermission(perm)) {
                        player.sendMessage(plugin.getMessage("teleports.warp.error.no_permission", warpName));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(warp.getLocation());
                    player.sendMessage(plugin.getMessage("teleports.warp.success", warpName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

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
                    player.sendMessage(plugin.getMessage("teleports.setwarp.success", warpName).color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("delwarp")
                .withArguments(new StringArgument("warpName"))
                .withFullDescription("Deletes the specified warp.")
                .withPermission("rapunzelcore.delwarp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    Location location = WarpsRepository.getWarpLocation(warpName);
                    if (location == null) {
                        player.sendMessage(plugin.getMessage("teleports.delwarp.error.invalid", warpName));
                        return Command.SINGLE_SUCCESS;
                    }
                    WarpsRepository.deleteWarp(warpName);
                    player.sendMessage(plugin.getMessage("teleports.delwarp.success", warpName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("warps")
                .withFullDescription("Lists all available warps.")
                .withPermission("rapunzelcore.warps")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    List<Warp> warps = WarpsRepository.getWarps();
                    if (warps.isEmpty()) {
                        player.sendMessage(plugin.getMessage("teleports.warps.error.none"));
                        return Command.SINGLE_SUCCESS;
                    }
                    Component message = plugin.getMessage("teleports.warps.header");
                    for (Warp warp : warps) {
                        String perm = warp.getPermission();
                        Location loc = warp.getLocation();
                        Component entry = plugin.getMessage("teleports.warps.entry",
                                warp.getName(),
                                String.valueOf(loc.getBlockX()),
                                String.valueOf(loc.getBlockY()),
                                String.valueOf(loc.getBlockZ())
                        );
                        if (perm != null) {
                            entry = entry.append(plugin.getMessage("teleports.warps.permission", perm));
                        }
                        message = message.appendNewline().append(entry);
                    }
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Spawn commands
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
                    player.sendMessage(plugin.getMessage("teleports.spawn.success"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("setspawn")
                .withFullDescription("Sets the spawn location for the current world.")
                .withPermission("rapunzelcore.setspawn")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    WarpsRepository.setSpawn(player.getWorld().getName(), player.getLocation());
                    player.sendMessage(plugin.getMessage("teleports.setspawn.success").color(NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }
}
