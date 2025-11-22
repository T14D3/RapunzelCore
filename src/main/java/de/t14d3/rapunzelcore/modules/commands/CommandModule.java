package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.Module;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

public class CommandModule implements Module {
    private boolean enabled = false;
    private Set<Command> registeredCommands = new HashSet<>();
    private FileConfiguration config;


    private String name(Class<? extends Command> clazz) {
        return clazz.getSimpleName().toLowerCase().replace("command", "");
    }

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        enabled = true;

        config = loadConfig(plugin);

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
    public void disable(Main plugin) {
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

}
