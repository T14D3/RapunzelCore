package de.t14d3.core.modules;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WarpsModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private File warpsFile;
    private FileConfiguration warpsConfig;
    private Map<String, Location> warps;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;
        warps = new HashMap<>();
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");

        try {
            if (!warpsFile.exists()) {
                warpsFile.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create warps.yml: " + e.getMessage());
        }

        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        loadWarps();

        registerCommands();
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        // CommandAPI commands are automatically unregistered on plugin disable, but we can clean up here if needed
        enabled = false;
    }

    @Override
    public String getName() {
        return "warps";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void registerCommands() {
        new CommandAPICommand("warp")
                .withArguments(new StringArgument("warpName")
                        .replaceSuggestions((info, builder) -> {
                            for (String key : warps.keySet()) {
                                if (info.sender().hasPermission("core.warp." + key)) {
                                    builder.suggest(key);
                                }
                            }
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Teleports to the specified warp.")
                .withPermission("core.warp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    Location location = warps.get(warpName);
                    if (location != null) {
                        player.teleport(location);
                        player.sendMessage(plugin.getMessage("commands.warp.success", warpName));
                    } else {
                        player.sendMessage(plugin.getMessage("commands.warp.error.invalid", warpName));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("setwarp")
                .withArguments(new StringArgument("warpName"))
                .withFullDescription("Sets a warp at your current location.")
                .withPermission("core.setwarp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    Location loc = player.getLocation();
                    warps.put(warpName, loc);
                    warpsConfig.set(warpName, loc);
                    saveWarps();
                    player.sendMessage(plugin.getMessage("commands.setwarp.success", warpName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("delwarp")
                .withArguments(new StringArgument("warpName"))
                .withFullDescription("Deletes the specified warp.")
                .withPermission("core.delwarp")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String warpName = (String) args.get("warpName");
                    if (warps.remove(warpName) != null) {
                        warpsConfig.set(warpName, null);
                        saveWarps();
                        player.sendMessage(plugin.getMessage("commands.delwarp.success", warpName));
                    } else {
                        player.sendMessage(plugin.getMessage("commands.delwarp.error.invalid", warpName));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("warps")
                .withFullDescription("Lists all available warps.")
                .withPermission("core.warps")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    if (warps.isEmpty()) {
                        player.sendMessage(plugin.getMessage("commands.warps.error.none"));
                    } else {
                        Component message = plugin.getMessage("commands.warps.header");
                        for (String key : warps.keySet()) {
                            message = message.appendNewline().append(plugin.getMessage("commands.warps.entry",
                                    key,
                                    String.valueOf(warps.get(key).getBlockX()),
                                    String.valueOf(warps.get(key).getBlockY()),
                                    String.valueOf(warps.get(key).getBlockZ())
                            ));
                        }
                        player.sendMessage(message);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    private void loadWarps() {
        for (String key : warpsConfig.getKeys(false)) {
            Location loc = warpsConfig.getLocation(key);
            if (loc != null) {
                warps.put(key, loc);
            } else {
                plugin.getLogger().warning("Warp '" + key + "' could not be loaded (invalid location data).");
            }
        }
    }

    private void saveWarps() {
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save warps: " + e.getMessage());
        }
    }
}
