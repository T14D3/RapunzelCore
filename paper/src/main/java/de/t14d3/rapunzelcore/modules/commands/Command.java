package de.t14d3.rapunzelcore.modules.commands;

import dev.jorel.commandapi.CommandAPI;

public interface Command {
    int SINGLE_SUCCESS = 1;

    void register();
    default void unregister() {
        CommandAPI.unregister(getName());
    }
    default String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("command", "");
    }
}
