package de.t14d3.rapunzelcore.commands;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;

public class ReloadCommand {

    public static CommandAPICommand command() {
        return new CommandAPICommand("reload")
                .withFullDescription("Reloads the Core plugin configuration and messages.")
                .withPermission("rapunzelcore.reload")
                .executes((executor, args) -> {
                    RapunzelPaperCore plugin = (RapunzelPaperCore) RapunzelCore.getInstance();

                    plugin.reloadPlugin();

                    executor.sendMessage(plugin.getMessageHandler().getMessage("commands.reload.success"));
                    plugin.getLogger().info("Core plugin reloaded by " + executor.getName());

                    return Command.SINGLE_SUCCESS;
                });
    }
}
