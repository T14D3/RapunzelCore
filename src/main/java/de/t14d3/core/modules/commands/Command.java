package de.t14d3.core.modules.commands;

import dev.jorel.commandapi.CommandAPI;

import java.util.ArrayList;
import java.util.List;

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
