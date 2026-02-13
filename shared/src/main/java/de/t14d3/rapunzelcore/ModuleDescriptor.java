package de.t14d3.rapunzelcore;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Set;
import java.util.Objects;

/**
 * Immutable class holding module metadata and optional loaded instance.
 *
 * <p>ModuleDescriptor provides metadata about a module without requiring
 * instantiation, making module discovery efficient.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * public class ChatModule implements Module {
 *     @Override
 *     public String getName() {
 *
 *
 * public String name() {
 * return getName();
 * }
 *         return "chat";
 *     }
 *     // ... other methods
 * }
 *
 * // Registration without instantiation:
 * ModuleDescriptor descriptor = ModuleDescriptor.from(ChatModule.class);
 * }</pre>
 *
 * @see Module
 * @see ModuleManager
 */
public class ModuleDescriptor {

    private static final String DEFAULT_VERSION = "1.0";
    private static final String DEFAULT_AUTHOR = "RapunzelCore";
    private static final Environment DEFAULT_ENVIRONMENT = Environment.BOTH;

    private final String name;
    private final String version;
    private final String author;
    private final Environment environment;
    private final Class<? extends Module> moduleClass;
    private final Set<String> dependencies;

    // Volatile reference to loaded module instance (null when not enabled)
    private volatile Module loadedModule;

    /**
     * Creates a ModuleDescriptor from a module class by instantiating it
     * and calling its getName() and getEnvironment() methods.
     *
     * @param moduleClass The module class to describe
     * @return A new ModuleDescriptor with metadata from the module
     * @throws IllegalArgumentException if the class cannot be instantiated
     */
    public static ModuleDescriptor from(Class<? extends Module> moduleClass) {
        try {
            Module module = (Module) moduleClass.getDeclaredConstructor().newInstance();
            return new ModuleDescriptor(
                    module.getName(),
                    DEFAULT_VERSION,
                    DEFAULT_AUTHOR,
                    module.getEnvironment(),
                    (Class<? extends Module>) moduleClass,
                    module.getDependencies()
            );
        } catch (NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    "Module class " + moduleClass.getName() + " must have a no-arg constructor and implement Module",
                    e
            );
        }
    }

    /**
     * Creates a ModuleDescriptor with explicit values (for testing or dynamic modules).
     *
     * @param name        The module name
     * @param version     The module version
     * @param author      The module author
     * @param environment The supported environment
     * @param moduleClass The module class
     */
    public ModuleDescriptor(
            String name,
            String version,
            String author,
            Environment environment,
            Class<? extends Module> moduleClass,
            Set<String> dependencies
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module name cannot be null or blank");
        }
        this.name = name;

        if (version == null || version.isBlank()) {
            this.version = DEFAULT_VERSION;
        } else {
            this.version = version;
        }

        if (author == null || author.isBlank()) {
            this.author = DEFAULT_AUTHOR;
        } else {
            this.author = author;
        }

        if (environment == null) {
            this.environment = DEFAULT_ENVIRONMENT;
        } else {
            this.environment = environment;
        }

        if (moduleClass == null) {
            throw new IllegalArgumentException("Module class cannot be null");
        }
        this.moduleClass = moduleClass;
        this.dependencies = dependencies != null ? dependencies : java.util.Collections.emptySet();
    }


    public String getName() {
        return name;
    }

    public String name() {
        return getName();
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Class<? extends Module> getModuleClass() {
        return moduleClass;
    }

    /**
     * Get the dependencies required by this module.
     *
     * @return An unmodifiable set of module names this module depends on
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * Check if this module supports the given environment.
     *
     * @param targetEnvironment The environment to check
     * @return true if the module supports the environment
     */
    public boolean supports(Environment targetEnvironment) {
        return environment == Environment.BOTH || environment == targetEnvironment;
    }

    /**
     * Check if this module is currently loaded and enabled.
     *
     * @return true if a module instance is loaded and enabled
     */
    public boolean isEnabled() {
        Module instance = loadedModule;
        return instance != null && instance.isEnabled();
    }

    /**
     * Get the loaded module instance, if any.
     *
     * @return The loaded module, or null if not enabled
     */
    public Module getLoadedModule() {
        return loadedModule;
    }

    /**
     * Create a new instance of this module.
     *
     * @return A new module instance, or null if instantiation fails
     */
    public Module createInstance() {
        try {
            return moduleClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException |
                 InstantiationException | IllegalAccessException e) {
            RapunzelCore.getLogger().error(
                    "Failed to instantiate module {}: {}",
                    name, e.getMessage(), e
            );
            return null;
        }
    }

    /**
     * Cache a loaded module instance.
     *
     * @param module The loaded module instance
     */
    void cacheLoaded(Module module) {
        this.loadedModule = module;
    }

    /**
     * Clear the cached module instance.
     */
    void clearLoaded() {
        this.loadedModule = null;
    }

    /**
     * Get the lookup key for this module (lowercase name).
     *
     * @return The lookup key
     */
    String getLookupKey() {
        return name.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleDescriptor that = (ModuleDescriptor) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(author, that.author) &&
                environment == that.environment &&
                Objects.equals(moduleClass, that.moduleClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, author, environment, moduleClass);
    }

    @Override
    public String toString() {
        return "ModuleDescriptor{" +
                "name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", author='" + author + '\'' +
                ", environment=" + environment +
                ", moduleClass=" + moduleClass +
                '}';
    }
}
