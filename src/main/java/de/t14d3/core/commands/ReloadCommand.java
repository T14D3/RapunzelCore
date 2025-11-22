package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;

public class ReloadCommand {

    public static CommandAPICommand command() {
        return new CommandAPICommand("reload")
                .withFullDescription("Reloads the Core plugin configuration and messages.")
                .withPermission("core.reload")
                .executes((executor, args) -> {
                    Main plugin = Main.getInstance();

                    plugin.reloadPlugin();

                    executor.sendMessage(plugin.getMessage("commands.reload.success"));
                    plugin.getLogger().info("Core plugin reloaded by " + executor.getName());

                    return Command.SINGLE_SUCCESS;
                });
    }
}
