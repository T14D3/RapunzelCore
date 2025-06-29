package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class PingCommand {
    public PingCommand() {
        new CommandAPICommand("ping")
                .withFullDescription("Shows the player's ping.")
                .withPermission("core.ping")
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("core.ping.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.ping.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    sender.sendMessage(Main.getInstance().getMessage("commands.ping.success", String.valueOf(target.getPing())));
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
