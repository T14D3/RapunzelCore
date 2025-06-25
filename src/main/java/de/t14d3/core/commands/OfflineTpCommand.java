package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class OfflineTpCommand {
    public OfflineTpCommand() {
        new CommandAPICommand("offlinetp")
                .withAliases("otp")
                .withPermission("core.offlinetp")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .replaceSuggestions((sender, builder) -> {
                            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                                builder.suggest(player.getName());
                            }
                            return builder.buildFuture();
                        })
                        .executes((executor, args) -> {
                            Player sender = (Player) executor;
                            OfflinePlayer target = Bukkit.getOfflinePlayerIfCached((String) args.get("player"));
                            if (target == null) {
                                sender.sendMessage(Main.getInstance().getMessage("commands.offlinetp.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            Location location = target.getLocation();
                            if (location == null) {
                                sender.sendMessage(Main.getInstance().getMessage("commands.offlinetp.error.no_location"));
                                return Command.SINGLE_SUCCESS;
                            }
                            sender.teleport(location);
                            Component message = Main.getInstance().getMessage("commands.offlinetp.success",
                                            target.getName())
                                    .color(NamedTextColor.GREEN);
                            sender.sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Teleports the player to the last known location of the given offline player.")
                .register(Main.getInstance());
    }
}
