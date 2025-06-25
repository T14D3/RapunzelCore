package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class SpawnCommand {
    public SpawnCommand() {
        new CommandAPICommand("spawn")
                .withFullDescription("Teleports the player to the server's spawn location.")
                .withPermission("core.spawn")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    player.teleport(player.getWorld().getSpawnLocation());
                    Component message = Main.getInstance().getMessage("commands.spawn.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
