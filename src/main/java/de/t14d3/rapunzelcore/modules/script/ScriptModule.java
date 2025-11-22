package de.t14d3.rapunzelcore.modules.script;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.Module;
import dev.jorel.commandapi.CommandAPI;
import org.bukkit.configuration.file.FileConfiguration;

public class ScriptModule implements Module {
    private ScriptManager scriptManager;
    private boolean enabled = false;
    private FileConfiguration config;


    @Override
    public void enable(Main plugin) {
        if (enabled) return;

        // Load or create config
        config = loadConfig(plugin);

        scriptManager = new ScriptManager(plugin, this);
        // Register commands that depend on ScriptManager
        new AliasCommand(scriptManager);
        new ScriptCommand(scriptManager);
        // Load aliases from config
        scriptManager.loadAliases(config);
        enabled = true;
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        enabled = false;

        CommandAPI.unregister("alias");
        CommandAPI.unregister("script");

        scriptManager.saveAliases();
        saveConfig(plugin, config);
    }


    @Override
    public String getName() {
        return "script";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
