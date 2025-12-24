package de.t14d3.rapunzelcore.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CoreCommand {
    public CoreCommand() {
        new CommandAPICommand("rapunzelcore")
                .withPermission("rapunzelcore.admin")
                .withSubcommands(
                        ReloadCommand.command(),
                        ModulesCommand.command(),
                        ConfigSyncCommand.command()
                )
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
