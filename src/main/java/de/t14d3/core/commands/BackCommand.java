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
                .withFullDescription("Teleports the player back to their last death location.")
                .withPermission("core.back")
                .executesPlayer((player, args) -> {
                    Location lastLocation = player.getLastDeathLocation();
                    if (lastLocation == null) {
                        player.sendMessage(Main.getInstance().getMessage("commands.back.error.no_location"));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(lastLocation);
                    Component message = Main.getInstance().getMessage("commands.back.success")
                            .color(NamedTextColor.GREEN);
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
