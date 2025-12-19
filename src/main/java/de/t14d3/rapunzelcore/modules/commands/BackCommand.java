package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackCommand implements Listener, Command {
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    @Override
    public void register() {
        new CommandAPICommand("back")
                .withFullDescription("Teleports the player back to their last death or teleport location.")
                .withPermission("rapunzelcore.back")
                .executesPlayer((player, args) -> {
                    Location target = lastLocations.get(player.getUniqueId());
                    if (target == null) {
                        target = player.getLastDeathLocation();
                    }
                    if (target == null) {
                        player.sendMessage(
                                Main.getInstance().getMessageHandler().getMessage("commands.back.error.no_location")
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(target);
                    Component message = Main.getInstance()
                            .getMessageHandler()
                            .getMessage("commands.back.success")
                            .color(NamedTextColor.GREEN);
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }

    @Override
    public void unregister() {
        PlayerTeleportEvent.getHandlerList().unregister(this);
        PlayerDeathEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        lastLocations.put(player.getUniqueId(), from);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();
        lastLocations.put(player.getUniqueId(), deathLoc);
    }
}
