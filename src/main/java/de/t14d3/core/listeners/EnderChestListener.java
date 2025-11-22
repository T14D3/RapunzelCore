package de.t14d3.core.listeners;

import de.t14d3.core.Main;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import static de.t14d3.core.modules.commands.EnderChestCommand.ENDER_CHEST_HOLDER;

public class EnderChestListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        handle(e.getWhoClicked(), e.getInventory(), e);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        handle(e.getWhoClicked(), e.getInventory(), e);
    }

    private void handle(HumanEntity human, Inventory top, Cancellable evt) {
        if (!(human instanceof Player)) return;
        Player viewer = (Player) human;

        // Only target EnderChest GUIs
        if (top.getType() != InventoryType.ENDER_CHEST) return;
        InventoryHolder holder = top.getHolder();
        if (holder == ENDER_CHEST_HOLDER) {
            evt.setCancelled(true);
            viewer.sendMessage(Main.getInstance().getMessage("commands.enderchest.error.modify"));
        }
    }

    public void unregister() {
        InventoryClickEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
        InventoryCloseEvent.getHandlerList().unregister(this);
    }
}
