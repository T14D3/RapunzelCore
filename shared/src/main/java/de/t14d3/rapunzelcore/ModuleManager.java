package de.t14d3.rapunzelcore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages module registration, enabling, and disabling.
 *
 * <p>This class is now instance-based (not static) and is associated with
 * a specific RapunzelCore instance. Each platform plugin should create its
 * own ModuleManager instance.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyPlugin extends RapunzelCore {
 *     private final ModuleManager moduleManager = new ModuleManager(this);
 *
 *     @Override
 *     public void onEnable() {
 *         // Register modules
 *         moduleManager.register(ChatModule.class);
 *         moduleManager.register(InventoryModule.class);
 *
 *         // Enable modules for this environment
 *         moduleManager.enable("ChatModule");
 *         moduleManager.enable("InventoryModule");
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         moduleManager.disableAll();
 *     }
 * }
 * }</pre>
 *
 * @see Module
 * @see ModuleDescriptor
 * @see RapunzelCore
 */
public class ModuleManager {

    private final RapunzelCore core;
    private final Map<String, ModuleDescriptor> modules;
    private final Map<String, Set<String>> reverseDependencies;

    /**
     * Creates a new ModuleManager for the given RapunzelCore instance.
     *
     * @param core The RapunzelCore instance to associate with this manager
     */
    public ModuleManager(@NotNull RapunzelCore core) {
        this.core = core;
        this.modules = new LinkedHashMap<>();
        this.reverseDependencies = new LinkedHashMap<>();
    }

    /**
     * Gets the RapunzelCore instance associated with this manager.
     *
     * @return The core instance
     */
    public @NotNull RapunzelCore getCore() {
        return core;
    }

    /**
     * Registers a module class with this manager.
     *
     * @param moduleClass The module class to register
     * @return The created ModuleDescriptor, or null if registration failed
     */
    public @Nullable ModuleDescriptor register(@NotNull Class<? extends Module> moduleClass) {
        try {
            ModuleDescriptor descriptor = ModuleDescriptor.from(moduleClass);
            modules.put(descriptor.getLookupKey(), descriptor);

            // Track reverse dependencies
            for (String dep : descriptor.getDependencies()) {
                String depKey = dep.toLowerCase(Locale.ROOT);
                reverseDependencies.computeIfAbsent(depKey, k -> new HashSet<>()).add(descriptor.getLookupKey());
            }

            RapunzelCore.getLogger().info("Registered module: {}", descriptor.getName());
            return descriptor;
        } catch (IllegalArgumentException e) {
            RapunzelCore.getLogger().error("Failed to register module {}", moduleClass.getName(), e);
            return null;
        }
    }

    /**
     * Enables a module by name.
     *
     * @param moduleName The name of the module to enable
     * @return true if the module was enabled successfully
     */
    public boolean enable(@NotNull String moduleName) {
        ModuleDescriptor descriptor = findDescriptor(moduleName);
        if (descriptor == null) {
            RapunzelCore.getLogger().warn("Module not found: {}", moduleName);
            return false;
        }

        Environment environment = core.getEnvironment();
        if (!descriptor.supports(environment)) {
            RapunzelCore.getLogger().debug(
                "Module {} does not support environment {}",
                descriptor.getName(), environment
            );
            return false;
        }

        // Check dependencies
        Set<String> missingDeps = new HashSet<>();
        for (String dep : descriptor.getDependencies()) {
            if (!isEnabled(dep)) {
                missingDeps.add(dep);
            }
        }
        if (!missingDeps.isEmpty()) {
            RapunzelCore.getLogger().warn(
                "Cannot enable module {}: missing required dependencies: {}",
                descriptor.getName(), String.join(", ", missingDeps)
            );
            return false;
        }

        // Check if already enabled
        if (descriptor.isEnabled()) {
            RapunzelCore.getLogger().debug("Module {} is already enabled", descriptor.getName());
            return true;
        }

        Module module = descriptor.createInstance();
        if (module == null) {
            // Error already logged by createInstance()
            return false;
        }

        try {
            descriptor.cacheLoaded(module);
            module.enable(core);

            // Update config
            core.getConfiguration().set("modules." + descriptor.getName(), true);
            core.saveConfig();

            RapunzelCore.getLogger().info("Enabled module: {}", descriptor.getName());
            return true;
        } catch (Exception e) {
            RapunzelCore.getLogger().error(
                "Failed to enable module: {}", descriptor.getName(), e
            );
            descriptor.clearLoaded();
            return false;
        }
    }

    /**
     * Disables a module by name.
     *
     * @param moduleName The name of the module to disable
     * @return true if the module was disabled (or was not enabled)
     */
    public boolean disable(@NotNull String moduleName) {
        ModuleDescriptor descriptor = findDescriptor(moduleName);
        if (descriptor == null) {
            RapunzelCore.getLogger().warn("Module not found: {}", moduleName);
            return false;
        }

        Module module = descriptor.getLoadedModule();
        if (module != null) {
            try {
                module.disable();
                RapunzelCore.getLogger().info("Disabled module: {}", descriptor.getName());
            } catch (Exception e) {
                RapunzelCore.getLogger().error(
                    "Error disabling module: {}", descriptor.getName(), e
                );
            }
        }

        descriptor.clearLoaded();

        // Warn about modules that depend on this one
        Set<String> dependents = reverseDependencies.get(descriptor.getLookupKey());
        if (dependents != null && !dependents.isEmpty()) {
            RapunzelCore.getLogger().warn(
                "Module {} has been disabled. The following modules depend on it and may not function correctly: {}",
                descriptor.getName(), String.join(", ", dependents)
            );
        }

        // Update config
        core.getConfiguration().set("modules." + descriptor.getName(), false);
        core.saveConfig();

        return true;
    }

    /**
     * Reloads a module by disabling and re-enabling it.
     *
     * @param moduleName The name of the module to reload
     * @return true if the module was reloaded successfully
     */
    public boolean reload(@NotNull String moduleName) {
        ModuleDescriptor descriptor = findDescriptor(moduleName);
        if (descriptor == null) {
            RapunzelCore.getLogger().warn("Module not found: {}", moduleName);
            return false;
        }

        Environment environment = core.getEnvironment();
        if (!descriptor.supports(environment)) {
            RapunzelCore.getLogger().debug(
                "Module {} does not support environment {}",
                descriptor.getName(), environment
            );
            return false;
        }

        // Check dependencies
        Set<String> missingDeps = new HashSet<>();
        for (String dep : descriptor.getDependencies()) {
            if (!isEnabled(dep)) {
                missingDeps.add(dep);
            }
        }
        if (!missingDeps.isEmpty()) {
            RapunzelCore.getLogger().warn(
                "Cannot enable module {}: missing required dependencies: {}",
                descriptor.getName(), String.join(", ", missingDeps)
            );
            return false;
        }

        // Disable if enabled
        Module currentModule = descriptor.getLoadedModule();
        if (currentModule != null) {
            try {
                currentModule.disable();
            } catch (Exception e) {
                RapunzelCore.getLogger().error(
                    "Error disabling module during reload: {}", descriptor.getName(), e
                );
            }
            descriptor.clearLoaded();
        }

        // Re-enable
        return enable(moduleName);
    }

    /**
     * Disables all enabled modules.
     */
    public void disableAll() {
        for (ModuleDescriptor descriptor : modules.values()) {
            if (descriptor.isEnabled()) {
                disable(descriptor.getName());
            }
        }
    }

    /**
     * Gets all registered module descriptors.
     *
     * @return An unmodifiable list of all registered descriptors
     */
    public @NotNull List<ModuleDescriptor> getModules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    /**
     * Gets all enabled module instances.
     *
     * @return A list of enabled module instances
     */
    public @NotNull List<Module> getEnabledModules() {
        List<Module> enabled = new ArrayList<>();
        for (ModuleDescriptor descriptor : modules.values()) {
            Module module = descriptor.getLoadedModule();
            if (module != null && module.isEnabled()) {
                enabled.add(module);
            }
        }
        return Collections.unmodifiableList(enabled);
    }

    /**
     * Gets a module by its class type.
     *
     * @param clazz The module class
     * @param <T> The module type
     * @return The enabled module instance
     * @throws IllegalArgumentException if the module is not found or not enabled
     */
    public @NotNull <T extends Module> T getModule(@NotNull Class<T> clazz) {
        for (ModuleDescriptor descriptor : modules.values()) {
            Module module = descriptor.getLoadedModule();
            if (module != null && clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        throw new IllegalArgumentException(
            "Module not found or not enabled: " + clazz.getName()
        );
    }

    /**
     * Gets a module descriptor by name.
     *
     * @param moduleName The module name
     * @return The descriptor, or null if not found
     */
    public @Nullable ModuleDescriptor getDescriptor(@NotNull String moduleName) {
        return findDescriptor(moduleName);
    }

    /**
     * Checks if a module is registered.
     *
     * @param moduleName The module name
     * @return true if the module is registered
     */
    public boolean isRegistered(@NotNull String moduleName) {
        return findDescriptor(moduleName) != null;
    }

    /**
     * Checks if a module is enabled.
     *
     * @param moduleName The module name
     * @return true if the module is enabled
     */
    public boolean isEnabled(@NotNull String moduleName) {
        ModuleDescriptor descriptor = findDescriptor(moduleName);
        return descriptor != null && descriptor.isEnabled();
    }

    private @Nullable ModuleDescriptor findDescriptor(@NotNull String moduleName) {
        return modules.get(moduleName.toLowerCase(Locale.ROOT));
    }
}
