package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class DelHomeCommand {

    public DelHomeCommand(Main plugin) {
        new CommandAPICommand("delhome")
                .withFullDescription("Deletes the player's set home location.")
                .withPermission("core.delhome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location homeLocation = plugin.getCommandManager().getHomeLocation(player);
                    if (homeLocation == null) {
                        player.sendMessage(Main.getInstance().getMessage("commands.delhome.error.no_home"));
                        return Command.SINGLE_SUCCESS;
                    }
                    plugin.getCommandManager().setHomeLocation(player, null);
                    player.sendMessage(Main.getInstance().getMessage("commands.delhome.success"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
