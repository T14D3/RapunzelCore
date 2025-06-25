package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class FlyCommand {
    public FlyCommand() {
        new CommandAPICommand("fly")
                .withAliases("f")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.fly.others")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                        .executes((executor, args) -> {
                            Player sender = (Player) executor;
                            Player target = (Player) args.get("player");
                            if (target == null) {
                                sender.sendMessage(Main.getInstance().getMessage("commands.fly.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            boolean enabled = target.getAllowFlight();
                            if (enabled) {
                                target.setFlying(false);
                            } else {
                                //noinspection deprecation
                                if (target.isOnGround()) {
                                    target.setFlying(true);
                                }
                            }
                            target.setAllowFlight(!enabled);
                            Component message = Main.getInstance().getMessage("commands.fly.toggle",
                                            target.getName(), !enabled ? "enabled" : "disabled");
                            sender.sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Toggles flight mode for the given player.")
                .withPermission("core.fly")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    boolean enabled = player.getAllowFlight();
                    player.setAllowFlight(!enabled);
                    Component message = Main.getInstance().getMessage("commands.fly.toggle",
                                    player.getName(), !enabled ? "enabled" : "disabled");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
