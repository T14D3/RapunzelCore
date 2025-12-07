package de.t14d3.rapunzelcore.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;

public class CoreCommand {
    public CoreCommand() {
        new CommandAPICommand("rapunzelcore")
                .withPermission("rapunzelcore.admin")
                .withSubcommands(
                        ReloadCommand.command(),
                        ModulesCommand.command()
                )
                .register(Main.getInstance());
    }
}
