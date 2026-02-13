package de.t14d3.rapunzelcore.modules.portals;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/**
 * Listener for detecting entities entering portals.
 */
public class PortalListener implements Listener {

    private final PaperPortalService portalService;

    public PortalListener(PaperPortalService portalService) {
        this.portalService = portalService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSignificantMove(event.getFrom(), event.getTo())) {
            portalService.onEntityMove(event.getPlayer(), event.getFrom(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Handle teleport events that might land in a portal
        portalService.onEntityMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        portalService.onEntityMove(event.getEntity(), event.getFrom(), event.getTo());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (isSignificantMove(event.getFrom(), event.getTo())) {
            // Check passengers
            for (Entity passenger : event.getVehicle().getPassengers()) {
                portalService.onEntityMove(passenger, event.getFrom(), event.getTo());
            }
        }
    }

    private boolean isSignificantMove(Location from, Location to) {
        if (to == null) return false;
        return from.getBlockX() != to.getBlockX()
            || from.getBlockY() != to.getBlockY()
            || from.getBlockZ() != to.getBlockZ();
    }
}
