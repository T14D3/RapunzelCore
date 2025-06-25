package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;

public class SocialSpyCommand {

    public SocialSpyCommand() {
        new CommandAPICommand("socialspy")
                .withAliases("spy")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.socialspy")
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
                                sender.sendMessage(Main.getInstance().getMessage("commands.socialspy.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            boolean enabled = sender.hasMetadata("socialspy");
                            if (enabled) {
                                sender.removeMetadata("socialspy", Main.getInstance());
                            } else {
                                sender.setMetadata("socialspy", new FixedMetadataValue(Main.getInstance(), true));
                            }
                            Component message = Main.getInstance().getMessage("commands.socialspy.toggle",
                                            target.getName(), !enabled ? "enabled" : "disabled");
                            sender.sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Toggles social spy mode for the given player.")
                .withPermission("core.socialspy");
    }
}
