package de.t14d3.rapunzelcore.modules.script;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;


import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.config.SnakeYamlConfig;
import de.t14d3.rapunzellib.config.YamlConfig;
import dev.jorel.commandapi.CommandAPI;

import de.t14d3.rapunzellib.Rapunzel;

import de.t14d3.rapunzellib.config.ConfigService;

import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ScriptModule implements Module, Listener {
    private RapunzelCore core;
    private boolean enabled = false;

    private ScriptManager scriptManager;
    private YamlConfig config;

    public Environment getEnvironment() {
        return Environment.PAPER;
    }

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
    public void enable(RapunzelCore core) {
        enable(core, getEnvironment());
    }
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        enabled = false;

        CommandAPI.unregister("alias");
        CommandAPI.unregister("script");

        scriptManager.saveAliases();
        saveConfig(config);
    }


    @Override
    public void disable() {
        disable(core, getEnvironment());
    }
    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return "script";
    }

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
