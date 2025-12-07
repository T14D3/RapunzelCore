package de.t14d3.rapunzelcore.modules;


import de.t14d3.rapunzelcore.Main;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();
    private static final Main plugin = Main.getInstance();


    public static void register(Module module) {
        modules.remove(module);
        modules.add(module);
    }

    public static boolean enable(String moduleName) {
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        module.enable(plugin);
        Main.getInstance().getConfig().set("modules." + moduleName, true);
        Main.getInstance().saveConfig();
        return true;
    }

    public static boolean disable(String moduleName) {
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        module.disable(plugin);
        Main.getInstance().getConfig().set("modules." + moduleName, false);
        Main.getInstance().saveConfig();
        return true;
    }

    public static boolean reload(String moduleName) {
        Module module = modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
        if (module == null) {
            return false;
        }
        module.disable(plugin);
        module.enable(plugin);
        return true;
    }

    public static List<Module> getModules() {
        return modules;
    }
}
