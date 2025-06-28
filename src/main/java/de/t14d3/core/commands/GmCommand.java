package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class GmCommand {
    public GmCommand() {
        new CommandAPICommand("gm")
                .withFullDescription("Changes the gamemode of the player.")
                .withPermission("core.gm")
                .withArguments(new StringArgument("gamemode")
                        .replaceSuggestions((sender, builder) -> {
                            builder.suggest("survival");
                            builder.suggest("creative");
                            builder.suggest("adventure");
                            builder.suggest("spectator");
                            builder.suggest("0");
                            builder.suggest("1");
                            builder.suggest("2");
                            builder.suggest("3");
                            return builder.buildFuture();
                        }))
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.gm.others")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = (Player) args.get("player");
                    if (target == null) {
                        target = sender;
                    }
                    String gamemode = (String) args.get("gamemode");

                    if (target == null) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.gm.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }
                    try {
                        int gamemodeInt = Integer.parseInt(gamemode);
                        //noinspection ConstantConditions
                        target.setGameMode(GameMode.getByValue(gamemodeInt));
                    } catch (IllegalArgumentException e) {
                        try {
                            target.setGameMode(GameMode.valueOf(gamemode.toUpperCase()));
                        } catch (IllegalArgumentException e1) {
                            sender.sendMessage(Main.getInstance().getMessage("commands.gm.error.invalid", gamemode));
                            return Command.SINGLE_SUCCESS;
                        }
                    }
                    Component message = Main.getInstance().getMessage("commands.gm.success",
                                    target.getName(), gamemode);
                    sender.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
