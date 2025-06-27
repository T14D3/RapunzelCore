package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class SetSpawnCommand {
    public SetSpawnCommand(Main plugin) {
        new CommandAPICommand("setspawn")
                .withFullDescription("Sets the worlds spawn location.")
                .withPermission("core.setspawn")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    plugin.setSpawn(player.getWorld(), player.getLocation());
                    Component message = plugin.getMessage("commands.setspawn.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
