package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;

public class BackCommand {

    public BackCommand() {
        new CommandAPICommand("back")
                .withFullDescription("Teleports the player back to their last death or teleport location.")
                .withPermission("core.back")
                .executesPlayer((player, args) -> {
                    Location target = Main.getInstance().getLastLocation(player.getUniqueId());
                    if (target == null) {
                        target = player.getLastDeathLocation();
                    }
                    if (target == null) {
                        player.sendMessage(
                                Main.getInstance().getMessage("commands.back.error.no_location")
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(target);
                    Component message = Main.getInstance()
                            .getMessage("commands.back.success")
                            .color(NamedTextColor.GREEN);
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}