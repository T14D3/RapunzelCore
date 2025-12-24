package de.t14d3.rapunzelcore.modules.script;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzellib.config.YamlConfig;
import dev.jorel.commandapi.CommandAPI;

import java.util.Map;

public class ScriptModule implements Module {
    private ScriptManager scriptManager;
    private boolean enabled = false;
    private YamlConfig config;

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        if (environment != Environment.PAPER) return;
        RapunzelPaperCore plugin = (RapunzelPaperCore) core;

        // Load or create config
        config = loadConfig();

        scriptManager = new ScriptManager(plugin, this);
        // Register commands that depend on ScriptManager
        new AliasCommand(scriptManager);
        new ScriptCommand(scriptManager);
        // Load aliases from config
        scriptManager.loadAliases(config);
        enabled = true;
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        enabled = false;

        CommandAPI.unregister("alias");
        CommandAPI.unregister("script");

        scriptManager.saveAliases();
        saveConfig(config);
    }


    @Override
    public String getName() {
        return "script";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.script", "op"),
                Map.entry("rapunzelcore.alias", "op"),
                Map.entry("rapunzelcore.script.block.*", "op")
        );
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public YamlConfig getConfig() {
        return config;
    }
}
