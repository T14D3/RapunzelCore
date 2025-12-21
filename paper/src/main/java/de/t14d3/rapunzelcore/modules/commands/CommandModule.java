package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandModule implements Module {
    private boolean enabled = false;
    private Set<Command> registeredCommands = new HashSet<>();
    private FileConfiguration config;

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    private String name(Class<? extends Command> clazz) {
        return clazz.getSimpleName().toLowerCase().replace("command", "");
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        if (environment != Environment.PAPER) return;
        enabled = true;
        RapunzelPaperCore plugin = (RapunzelPaperCore) core;

        config = loadConfig();

        Set<Class<? extends Command>> commands = ReflectionsUtil.getSubTypes(Command.class);
        commands.forEach(clazz -> {
            if (!config.getKeys(false).contains(name(clazz))) {
                config.set(name(clazz), true);
            }
        });

        registeredCommands = ReflectionsUtil.instantiateSubTypes(Command.class, commands,
                command -> (config.getBoolean(name(command), true)),"register"
        );
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        // Unregister all registered commands
        for (Command command : registeredCommands) {
            command.unregister();
        }
        registeredCommands.clear();
        enabled = false;
    }

    @Override
    public String getName() {
        return "commands";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.anvil", "op"),
                Map.entry("rapunzelcore.back", "op"),
                Map.entry("rapunzelcore.craft", "op"),
                Map.entry("rapunzelcore.enderchest", "op"),
                Map.entry("rapunzelcore.enderchest.others", "op"),
                Map.entry("rapunzelcore.enderchest.edit.others", "op"),
                Map.entry("rapunzelcore.fly", "op"),
                Map.entry("rapunzelcore.fly.others", "op"),
                Map.entry("rapunzelcore.flyspeed.others", "op"),
                Map.entry("rapunzelcore.gm", "op"),
                Map.entry("rapunzelcore.gm.others", "op"),
                Map.entry("rapunzelcore.god", "op"),
                Map.entry("rapunzelcore.god.others", "op"),
                Map.entry("rapunzelcore.heal", "op"),
                Map.entry("rapunzelcore.heal.others", "op"),
                Map.entry("rapunzelcore.invsee", "op"),
                Map.entry("rapunzelcore.invsee.modify", "op"),
                Map.entry("rapunzelcore.maintenance", "op"),
                Map.entry("rapunzelcore.maintenance.bypass", "op"),
                Map.entry("rapunzelcore.nick", "op"),
                Map.entry("rapunzelcore.ping", "op"),
                Map.entry("rapunzelcore.ping.others", "op"),
                Map.entry("rapunzelcore.playtime", "op"),
                Map.entry("rapunzelcore.playtime.others", "op"),
                Map.entry("rapunzelcore.repair", "op"),
                Map.entry("rapunzelcore.repair.others", "op"),
                Map.entry("rapunzelcore.restart", "op"),
                Map.entry("rapunzelcore.speed", "op"),
                Map.entry("rapunzelcore.uinfo", "op"),
                Map.entry("rapunzelcore.vanish", "op"),
                Map.entry("rapunzelcore.vanish.see", "op")
        );
    }

}
