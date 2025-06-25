package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TeamChatCommand {
    public TeamChatCommand() {
        new CommandAPICommand("tc")
                .withAliases("teamchat")
                .withPermission("core.teamchat.use")
                .withArguments(new StringArgument("message")
                        .withPermission("core.teamchat")
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    String raw = (String) args.get("message");
                    Set<Player> recipients = Bukkit.getOnlinePlayers().stream()
                            .filter(player -> player.hasPermission("core.teamchat.see"))
                            .collect(Collectors.toSet());
                    Component message = Main.getInstance().getMessage("commands.teamchat.format.sender",
                            sender.getName(), raw);
                    recipients.forEach(player -> {
                        player.sendMessage(message);
                    });

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sends a message to your team members.")
                .register(Main.getInstance());
    }
}
