package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class FlySpeedCommand {
    public FlySpeedCommand() {
        new CommandAPICommand("flyspeed")
                .withAliases("fs")
                .withArguments(
                        new FloatArgument("speed", 0.0F, 100.0F)
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("core.flyspeed")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(player -> {
                                        builder.suggest(player.getName());
                                    });
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? (Player) args.get("player") : sender;
                    float speed = (float) args.get("speed");

                    if (target == null) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.flyspeed.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    target.setFlySpeed(speed);
                    Component message = Main.getInstance().getMessage("commands.flyspeed.set",
                                    target.getName(), String.format("%.2f", speed))
                            .color(NamedTextColor.GREEN);
                    sender.sendMessage(message);

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sets the flight speed for the given player.")
                .register(Main.getInstance());
    }
}
