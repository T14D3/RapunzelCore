package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EnderChestModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private EnderChestListener listener;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;
        listener = new EnderChestListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;

        InventoryClickEvent.getHandlerList().unregister(listener);
        PlayerInteractEvent.getHandlerList().unregister(listener);

        enabled = false;
    }

    @Override
    public String getName() {
        return "enderchest";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Open the player's enderchest when clicking an ender chest item.
     */
    private void openEnderChest(Player player) {
        if (player.getOpenInventory().getTopInventory().getType() != org.bukkit.event.inventory.InventoryType.PLAYER) {
            player.closeInventory();
        }
        player.openInventory(player.getEnderChest());
    }

    private record EnderChestListener(EnderChestModule module) implements Listener {

        /**
         * Called when player middle/shift-clicks an ender chest in their inventory to open it.
         */
        @EventHandler
        public void onInventoryClickOpen(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;

            // only trigger when clicking inside player inventory (opening the item)
            if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.ENDER_CHEST) return;

            ClickType click = event.getClick();
            if (click != ClickType.MIDDLE && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;

            // Prevent default behavior and open enderchest
            event.setCancelled(true);
            module.openEnderChest(player);
        }

        /**
         * Intercepts right-click with ender chest item in hand.
         */
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.hasBlock()) return;
            if (event.getItem() == null) return;
            if (event.getItem().getType() != Material.ENDER_CHEST) return;

            event.setCancelled(true);
            module.openEnderChest(event.getPlayer());
        }
    }
    }

