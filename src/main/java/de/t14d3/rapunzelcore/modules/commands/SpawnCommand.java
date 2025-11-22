package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class SpawnCommand implements Command {
    private Map<World, Location> spawns = new HashMap<>();

    @Override
    public void register() {
        Main plugin = Main.getInstance();
        new CommandAPICommand("spawn")
                .withFullDescription("Teleports the player to the server's spawn location.")
                .withPermission("rapunzelcore.spawn")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player player)) return Command.SINGLE_SUCCESS;
                    player.teleport(spawns.get(player.getWorld()));
                    Component message = plugin.getMessage("commands.spawn.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
        new CommandAPICommand("setspawn")
                .withFullDescription("Sets the worlds spawn location.")
                .withPermission("rapunzelcore.setspawn")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player player)) return Command.SINGLE_SUCCESS;
                    spawns.put(player.getWorld(), player.getLocation());
                    Component message = plugin.getMessage("commands.setspawn.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
