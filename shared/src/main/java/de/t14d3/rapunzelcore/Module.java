package de.t14d3.rapunzelcore;

import java.nio.file.Path;
import java.util.Collections;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;

/**
 * Core module interface for RapunzelCore.
 *
 * <p>Modules are the primary extension mechanism for RapunzelCore. Each module
 * provides specific functionality and can be enabled or disabled independently.</p>
 *
 * <p>Modules should be designed to be environment-agnostic where possible, with
 * platform-specific implementations provided as needed.</p>
 *
 * @see ModuleManager
 * @see ModuleDescriptor
 */
public interface Module {

    /**
     * Get the unique name of this module.
     *
     * <p>The name should be a simple lowercase identifier (e.g., "chat", "inventories").
     * It is used for module lookup and configuration.</p>
     *
     * @return The module name
     */
    String getName();

    /**
     * Get the environment(s) this module supports.
     *
     * @return The supported environment(s)
     */
    Environment getEnvironment();

    /**
     * Enable this module.
     *
     * <p>This method is called when the module is being activated. The module
     * should initialize any resources it needs and register any event listeners
     * or commands.</p>
     *
     * @param core The RapunzelCore instance
     */
    void enable(RapunzelCore core);

    /**
     * Disable this module.
     *
     * <p>This method is called when the module is being deactivated. The module
     * should clean up any resources it allocated during enable().</p>
     */
    void disable();

    /**
     * Check if this module is currently enabled.
     *
     * @return true if the module has been enabled and not yet disabled
     */
    boolean isEnabled();

    /**
     * Get the RapunzelCore instance associated with this module.
     *
     * @return The RapunzelCore instance, or null if the module is not enabled
     */
    default RapunzelCore getCore() {
        return null;
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

    /**
     * Get the dependencies required by this module.
     *
     * <p>Dependencies are module names that must be enabled before this module
     * can be enabled. If any dependency is not enabled, this module will fail
     * to enable with a warning.</p>
     *
     * <p>Default implementation returns an empty set, meaning the module has
     * no dependencies.</p>
     *
     * @return A set of module names this module depends on
     */
    default java.util.Set<String> getDependencies() {
        return java.util.Collections.emptySet();
    }

}