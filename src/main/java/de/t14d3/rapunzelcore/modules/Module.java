package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public interface Module {

    void enable(Main plugin);

    void disable(Main plugin);

    String getName();

    boolean isEnabled();

    default File getConfigFile() {
        Main plugin = Main.getInstance();
        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
        File configFile = new File(modulesDir, getName() + ".yaml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create config file for module " + getName() + ": " + e.getMessage());
            }
        }
        return configFile;
    }

    default FileConfiguration loadConfig() {
        File configFile = getConfigFile();
        //noinspection DataFlowIssue
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getClass().getResourceAsStream("/modules/" + getName() + ".yaml"))
        );
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        defaultConfig.getKeys(true).forEach(key -> {
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
            }
        });
        saveConfig(config);
        return config;
    }

    default void saveConfig(FileConfiguration config) {
        File configFile = getConfigFile();
        try {
            config.save(configFile);
        } catch (IOException e) {
            Main.getInstance().getLogger().severe("Failed to save config for module " + getName() + ": " + e.getMessage());
        }
    }
}
