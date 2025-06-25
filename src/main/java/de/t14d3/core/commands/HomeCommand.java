package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class HomeCommand {

    public HomeCommand(Main plugin) {

        new CommandAPICommand("home")
                .withFullDescription("Teleports the player to their set home location.")
                .withPermission("core.home")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location homeLocation = plugin.getCommandManager().getHomeLocation(player);
                    if (homeLocation == null) {
                        player.sendMessage(Main.getInstance().getMessage("commands.home.error.no_home"));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(homeLocation);
                    Component message = Main.getInstance().getMessage("commands.home.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
