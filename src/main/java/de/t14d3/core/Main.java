package de.t14d3.core;

import de.t14d3.core.commands.CoreCommand;
import de.t14d3.core.modules.Module;
import de.t14d3.core.util.ReflectionsUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin {
    private MessageHandler messages;
    private static Main instance;
    private Map<String, Location> spawns = new HashMap<>();
    private Map<String, Module> modules = new HashMap<>();

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        messages = new MessageHandler(this);
        saveDefaultConfig();

        getServer().getWorlds().forEach(world -> {
            spawns.put(world.getName(), getConfig().getLocation("spawn." + world.getName()));
        });
        spawns.put("global", getConfig().getLocation("spawn.global", getServer().getWorlds().get(0).getSpawnLocation()));

        // Load modules from config
        loadModules();

        new CoreCommand();


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
        CommandAPI.onLoad(
                new CommandAPIPaperConfig((JavaPlugin) this)
                        .verboseOutput(true)
                        .silentLogs(false)
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

    public Location getSpawn(World world) {
        return spawns.get(world.getName()) == null ? spawns.get("global") : spawns.get(world.getName());
    }

    public void setSpawn(World world, Location location) {
        spawns.put(world.getName(), location);
    }


    public void reloadPlugin() {
        // Disable all modules
        for (Module module : modules.values()) {
            module.disable(this);
        }
        modules.clear();

        // Reload plugin configuration
        reloadConfig();

        // Reload messages
        messages.reloadMessages(this);

        // Reload spawns from config
        spawns.clear();
        getServer().getWorlds().forEach(world -> {
            spawns.put(world.getName(), getConfig().getLocation("spawn." + world.getName()));
        });
        spawns.put("global", getConfig().getLocation("spawn.global", getServer().getWorlds().get(0).getSpawnLocation()));

        // Load modules
        loadModules();
    }



    private void loadModules() {
        ReflectionsUtil.getSubTypes(Module.class).forEach(clazz -> {
            try {
                Module module = clazz.getDeclaredConstructor().newInstance();
                String name = module.getName();
                if (getConfig().isBoolean("modules." + name) && getConfig().getBoolean("modules." + name)) {
                    module.enable(this);
                    modules.put(name, module);
                    getLogger().info("Loaded module: " + name);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to load module " + clazz.getName() + ": " + e.getMessage());
            }
        });
    }
}
