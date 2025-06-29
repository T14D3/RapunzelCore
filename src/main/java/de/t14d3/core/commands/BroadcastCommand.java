package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastCommand {
    public BroadcastCommand() {
        new CommandAPICommand("broadcast")
                .withAliases("bc")
                .withArguments(new GreedyStringArgument("message"))
                .withFullDescription("Broadcasts a message to all online players.")
                .withPermission("core.broadcast")
                .executes((executor, args) -> {
                    String message = (String) args.get("message");
                    Component broadcastMessage = Main.getInstance().getMessage("commands.broadcast.format",
                            message, executor.getName());

                    if (message.contains("--title") && executor.hasPermission("core.broadcast.title")) {
                        Bukkit.getServer().showTitle(Title.title(broadcastMessage, Component.empty()));
                    }
                    Bukkit.broadcast(broadcastMessage);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
