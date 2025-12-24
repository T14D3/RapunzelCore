package de.t14d3.rapunzelcore;


import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;

import java.util.Collections;
import java.util.Map;
import java.nio.file.Path;

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
     * Returns the file path for this module's YAML config.
     */
    default Path getConfigPath() {
        RapunzelCore core = RapunzelCore.getInstance();
        return core.getDataFolder().toPath().resolve("modules").resolve(getName() + ".yaml");
    }

    /**
     * Load the configuration for this module.
     *
     * @return The loaded configuration
     */

    default YamlConfig loadConfig() {
        String defaultResource = "modules/" + getName() + ".yaml";
        return Rapunzel.context().configs().load(getConfigPath(), defaultResource);
    }

    /**
     * Save the configuration for this module.
     *
     * @param config The configuration to save
     */
    default void saveConfig(YamlConfig config) {
        if (config != null) config.save();
    }
}
