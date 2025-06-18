package de.bydennyy.byDennyysEssentials.listener;

import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

public class InventoryListener implements Listener {

    @Getter
    private final Plugin plugin;

    public InventoryListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event){

        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            // TODO: implement edit prevention if permission not granted
            return;
        }

    }
}
