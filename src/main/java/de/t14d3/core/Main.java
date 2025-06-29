package de.t14d3.core;

import de.t14d3.core.listeners.ChatListener;
import de.t14d3.core.listeners.EnderChestListener;
import de.t14d3.core.listeners.InvSeeListener;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {
    private MessageHandler messages;
    private static Main instance;
    private CommandManager commandManager;
    private Map<String, Location> spawns = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        messages = new MessageHandler(this);
        saveDefaultConfig();

        getServer().getWorlds().forEach(world -> {
            spawns.put(world.getName(), getConfig().getLocation("spawn." + world.getName()));
        });
        spawns.put("global", getConfig().getLocation("spawn.global", getServer().getWorlds().get(0).getSpawnLocation()));

        commandManager = new CommandManager(this);

        getServer().getPluginManager().registerEvents(new InvSeeListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new EnderChestListener(), this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File messagesFile = new File(getDataFolder(), "messages.properties");
        if (!messagesFile.exists()) {
            try {
                //noinspection DataFlowIssue // File is always included in jar
                Files.copy(getClass().getResourceAsStream("/messages.properties"), messagesFile.toPath());
            } catch (Exception e) {
                getLogger().severe("Failed to copy default messages.properties: " + e.getMessage());
            }
        }
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .verboseOutput(false)
                .skipReloadDatapacks(true)
                .silentLogs(true)
        );
        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        spawns.forEach((world, location) -> getConfig().set("spawn." + world, location));
        saveConfig();
    }

    public MessageHandler getMessages() {
        return messages;
    }

    public Component getMessage(String key) {
        return messages.getMessage(key);
    }

    public Component getMessage(String key, String... args) {
        return messages.getMessage(key, args);
    }

    public static Main getInstance() {
        return instance;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Location getSpawn(World world) {
        return spawns.get(world.getName()) == null ? spawns.get("global") : spawns.get(world.getName());
    }

    public void setSpawn(World world, Location location) {
        spawns.put(world.getName(), location);
    }

    public void setLastLocation(UUID playerId, Location location) {
        lastLocations.put(playerId, location);
    }

    public Location getLastLocation(UUID playerId) {
        return lastLocations.get(playerId);
    }


}
