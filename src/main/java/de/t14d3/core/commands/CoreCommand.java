package de.t14d3.core.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;

public class CoreCommand {
    public CoreCommand() {
        new CommandAPICommand("core")
                .withPermission("core.admin")
                .withSubcommand(ReloadCommand.command())
                .register(Main.getInstance());
    }
}
