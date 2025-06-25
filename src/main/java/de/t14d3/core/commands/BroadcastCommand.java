package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastCommand {
    public BroadcastCommand() {
        new CommandAPICommand("broadcast")
                .withAliases("bc")
                .withArguments(new GreedyStringArgument("message")
                        .withPermission("core.broadcast")
                        .executes((executor, args) -> {
                            String message = (String) args.get("message");
                            Component broadcastMessage = Main.getInstance().getMessage("commands.broadcast.format",
                                    executor.getName(), message);
                            Bukkit.broadcast(broadcastMessage);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Broadcasts a message to all online players.")
                .withPermission("core.broadcast")
                .register(Main.getInstance());
    }
}
