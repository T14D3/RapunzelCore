package de.t14d3.core.listeners;

import de.t14d3.core.util.PlayerInventoryMirror;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class InvSeeListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();

        PlayerInventoryMirror mirror = PlayerInventoryMirror.getActiveMirror(clicker.getUniqueId());

        // Check if there is an active mirror for this viewer AND the clicked inventory
        // is either the mirrored inventory or the viewer's own inventory while a mirror is open.
        if (mirror != null &&
                (event.getClickedInventory() != null && event.getClickedInventory().equals(mirror.getMirroredInventory()) ||
                        event.getInventory().equals(clicker.getInventory()) ||
                        event.getClick().isShiftClick()
                )) { // Check if the top inventory is the mirrored one, or if the bottom is the viewer's
            mirror.handleInventoryClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();

        PlayerInventoryMirror mirror = PlayerInventoryMirror.getActiveMirror(clicker.getUniqueId());

        if (mirror != null) {
            mirror.handleInventoryDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player closer = (Player) event.getPlayer();

        PlayerInventoryMirror mirror = PlayerInventoryMirror.getActiveMirror(closer.getUniqueId());

        // Only handle if the closed inventory is our mirrored inventory
        if (mirror != null && event.getInventory().equals(mirror.getMirroredInventory())) {
            mirror.handleInventoryClose(event);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Check if the quitting player is a viewer
        PlayerInventoryMirror viewerMirror = PlayerInventoryMirror.getActiveMirror(player.getUniqueId());
        if (viewerMirror != null) {
            viewerMirror.handlePlayerQuit(event);
        }

        for (PlayerInventoryMirror mirror : PlayerInventoryMirror.activeMirrors.values()) {
            if (mirror.target.equals(player) && !mirror.viewer.equals(player)) { // Ensure it's not the same player handled above
                mirror.handlePlayerQuit(event);
            }
        }
    }

    public void unregister() {
        PlayerQuitEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }
}
