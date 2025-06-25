package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;

public class VanishCommand {
    private static final Map<Player, Boolean> vanishedPlayers = new HashMap<>();

    public VanishCommand() {
        new CommandAPICommand("vanish")
                .withAliases("v")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.vanish")
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
                                sender.sendMessage(Main.getInstance().getMessage("commands.vanish.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            boolean vanished = vanishedPlayers.getOrDefault(target, false);
                            vanishedPlayers.put(target, !vanished);
                            Component message = Main.getInstance().getMessage("commands.vanish.toggle",
                                            target.getName(), !vanished ? "enabled" : "disabled")
                                    .color(vanished ? NamedTextColor.RED : NamedTextColor.GREEN);
                            sender.sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Toggles vanish mode for the given player.")
                .withPermission("core.vanish")
                .register(Main.getInstance());
    }
}
