package de.t14d3.rapunzelcore;


import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.Collections;
import java.util.Map;

/**
 * Core module interface that can be implemented by modules across all platforms.
 * Each platform (Paper, Velocity) should provide their own main plugin class
 * that can be passed to enable/disable methods.
 */
public interface Module {
    
    /**
     * Get the environment/platform this module is designed for.
     * 
     * @return The environment enum (PAPER, VELOCITY, or BOTH)
     */
    Environment getEnvironment();
    
    /**
     * Enable this module with the provided RapunzelCore instance.
     * 
     * @param core The RapunzelCore instance (Paper plugin, Velocity plugin, etc.)
     * @param environment The environment/platform where this module is being enabled
     */
    void enable(RapunzelCore core, Environment environment);
    
    /**
     * Disable this module with the provided RapunzelCore instance.
     * 
     * @param core The RapunzelCore instance (Paper plugin, Velocity plugin, etc.)
     * @param environment The environment/platform where this module is being disabled
     */
    void disable(RapunzelCore core, Environment environment);
    
    /**
     * Get the name of this module.
     * 
     * @return The module name
     */
    String getName();
    
    /**
     * Check if this module is currently enabled.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Returns the permissions used by this module.
     *
     * <p>Map keys are permission nodes, values are Bukkit-style defaults (e.g. "op", "true").</p>
     */
    default Map<String, String> getPermissions() {
        return Collections.emptyMap();
    }


    /**
     * Load the configuration for this module.
     *
     * @return The loaded configuration
     */
    default File getConfigFile() {
        RapunzelCore core = RapunzelCore.getInstance();
        File modulesDir = new File(core.getDataFolder(), "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
        File configFile = new File(modulesDir, getName() + ".yaml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                RapunzelCore.getLogger().error("Failed to create config file for module " + getName() + ": " + e.getMessage());
            }
        }
        return configFile;
    }

    /**
     * Load the configuration for this module.
     *
     * @return The loaded configuration
     */

    default FileConfiguration loadConfig() {
        FileConfiguration config;
        try {
            File configFile = getConfigFile();
            //noinspection DataFlowIssue
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(getClass().getResourceAsStream("/modules/" + getName() + ".yaml"))
            );
            config = YamlConfiguration.loadConfiguration(configFile);
            defaultConfig.getKeys(true).forEach(key -> {
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.get(key));
                }
            });
        } catch (IOException e) {
            RapunzelCore.getLogger().error("Failed to load config for module {}: {}", getName(), e.getMessage());
            return null;
        }
        saveConfig(config);
        return config;
    }

    /**
     * Save the configuration for this module.
     *
     * @param config The configuration to save
     */
    default void saveConfig(FileConfiguration config) {
        File configFile = getConfigFile();
        try {
            config.save(configFile);
        } catch (IOException e) {
            RapunzelCore.getLogger().error("Failed to save config for module {}: {}", getName(), e.getMessage());
        }
    }
}
