package de.t14d3.rapunzelcore.modules.commands;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class BroadcastCommand {
    public BroadcastCommand() {
        new CommandAPICommand("broadcast")
                .withAliases("bc")
                .withArguments(new GreedyStringArgument("message"))
                .withFullDescription("Broadcasts a message to all online players.")
                .withPermission("rapunzelcore.broadcast")
                .executes((executor, args) -> {
                    String message = (String) args.get("message");
                    Component broadcastMessage = Main.getInstance().getMessage("commands.broadcast.format",
                            message, executor.getName());
                    Bukkit.broadcast(broadcastMessage);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
