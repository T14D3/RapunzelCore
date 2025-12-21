package de.t14d3.rapunzelcore;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();

    public static void register(Module module) {
        modules.remove(module);
        modules.add(module);
        RapunzelCore core = RapunzelCore.getInstance();
        core.getPlatformManager().registerPermissions(module.getPermissions());
    }

    public static boolean enable(String moduleName, Environment environment) {
        RapunzelCore core = RapunzelCore.getInstance();
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        // Check if module is compatible with the current environment
        Environment moduleEnvironment = module.getEnvironment();
        if (moduleEnvironment == Environment.BOTH || moduleEnvironment == environment) {
            module.enable(core, environment);
            core.getConfiguration().set("modules." + moduleName, true);
            core.saveConfig();
            return true;
        }
        return false;
    }

    public static boolean disable(String moduleName, Environment environment) {
        RapunzelCore core = RapunzelCore.getInstance();
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        module.disable(core, environment);
        core.getConfiguration().set("modules." + moduleName, false);
        core.saveConfig();
        return true;
    }

    public static boolean reload(String moduleName, Environment environment) {
        RapunzelCore core = RapunzelCore.getInstance();
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        module.disable(core, environment);
        module.enable(core, environment);
        return true;
    }

    // Legacy methods for backward compatibility - these will be removed in future versions
    @Deprecated
    public static boolean enable(String moduleName) {
        RapunzelCore core = RapunzelCore.getInstance();
        return enable(moduleName, core.getEnvironment());
    }

    @Deprecated
    public static boolean disable(String moduleName) {
        return disable(moduleName, Environment.BOTH);
    }

    @Deprecated
    public static boolean reload(String moduleName) {
        return reload(moduleName, Environment.BOTH);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return clazz.cast(module);
            }
        }
        throw new IllegalArgumentException("Module not found");
    }
}
