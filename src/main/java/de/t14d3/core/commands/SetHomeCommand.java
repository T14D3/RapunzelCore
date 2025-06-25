package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class SetHomeCommand {
    private final HomeCommand homeCommand;

    public SetHomeCommand(Main plugin) {
        this.homeCommand = new HomeCommand(plugin);
        new CommandAPICommand("sethome")
                .withFullDescription("Sets the player's home location to their current location.")
                .withPermission("core.sethome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    homeCommand.setHomeLocation(player, player.getLocation());
                    Component message = Main.getInstance().getMessage("commands.sethome.success")
                            .color(NamedTextColor.GREEN);
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
