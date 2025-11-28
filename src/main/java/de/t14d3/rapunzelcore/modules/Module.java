package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public interface Module {
    List<Module> MODULES = new ArrayList<>();

    void enable(Main plugin);

    void disable(Main plugin);

    String getName();

    boolean isEnabled();

    default File getConfigFile(Main plugin) {
        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
        return new File(modulesDir, getName() + ".yaml");
    }

    default FileConfiguration loadConfig(Main plugin) {
        File configFile = getConfigFile(plugin);
        return YamlConfiguration.loadConfiguration(configFile);
    }

    default void saveConfig(Main plugin, FileConfiguration config) {
        File configFile = getConfigFile(plugin);
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config for module " + getName() + ": " + e.getMessage());
        }
    }
}
