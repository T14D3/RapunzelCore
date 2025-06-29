package de.t14d3.core.listeners;

import de.t14d3.core.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class LastLocationTracker implements Listener {
    private final Main plugin;

    public LastLocationTracker(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        plugin.setLastLocation(player.getUniqueId(), from);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLoc = player.getLocation();
        plugin.setLastLocation(player.getUniqueId(), deathLoc);
    }
}