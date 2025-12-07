package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShulkerModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private ShulkerListener listener;
    private static final NamespacedKey SESSION = new NamespacedKey(Main.getInstance(), "shulker_open_session");
    private static final ConcurrentHashMap<Player, ShulkerHolder> HOLDERS = new ConcurrentHashMap<>();

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;
        listener = new ShulkerListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;

        HandlerList.unregisterAll(listener);

        HOLDERS.forEach((player, holder) -> {
            // Close all open shulker views
            player.closeInventory();
            holder.getInventory().clear();
            holder.getInventory().getViewers().forEach(HumanEntity::closeInventory);

            // Remove session tag from container item
            ItemMeta meta = holder.containerItem.getItemMeta();
            meta.getPersistentDataContainer().remove(SESSION);
            holder.containerItem.setItemMeta(meta);
            HOLDERS.remove(player);
        });

        enabled = false;
    }

    @Override
    public String getName() {
        return "shulker";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Open an editable shulker view that synchronizes on every change.
     * The actual ItemStack instance is tagged with a session id so it can be identified,
     * and a holder keeps an immutable reference to the ItemStack.
     *
     * @param player      player opening
     * @param shulkerItem item in the player's inventory (the actual reference we will tag and hold)
     */
    private void openShulkerBox(Player player, ItemStack shulkerItem) {
        if (shulkerItem == null) return;
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

        // Create session id and tag the actual item so we can always find it and prevent moves.
        UUID sessionId = UUID.randomUUID();
        meta.getPersistentDataContainer().set(SESSION, PersistentDataType.STRING, sessionId.toString());
        shulkerItem.setItemMeta(meta); // write tag into the actual item in inventory

        // Create holder that keeps an immutable reference to the actual ItemStack and session id.
        ShulkerHolder holder = new ShulkerHolder(player.getUniqueId(), sessionId, shulkerItem);
        HOLDERS.put(player, holder);

        Inventory viewInv = Bukkit.createInventory(holder, InventoryType.SHULKER_BOX, shulkerItem.effectiveName());
        viewInv.setContents(shulker.getInventory().getContents());

        player.openInventory(viewInv);
    }

    /**
     * InventoryHolder used to attach owner, session id, and the actual item reference.
     *
     * @param containerItem immutable reference to the actual item the player had
     */
        private record ShulkerHolder(UUID owner, UUID sessionId, ItemStack containerItem) implements InventoryHolder {
            private static final Inventory inventory = Bukkit.createInventory(null, InventoryType.SHULKER_BOX);
            @Override
            public @NotNull Inventory getInventory() {
                return inventory;
            }
        }

    private record ShulkerListener(ShulkerModule module) implements Listener {

        /**
             * Called when player middle/shift-clicks a shulker in their inventory to open it.
             * Cancels the normal click and opens the virtual editable view.
             */
            @EventHandler
            public void onInventoryClickOpen(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) return;

                // only trigger when clicking inside player inventory (opening the item)
                if (!(event.getClickedInventory() instanceof PlayerInventory)) return;

                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || !isShulkerBox(clicked.getType())) return;

                ClickType click = event.getClick();
                if (click != ClickType.MIDDLE && click != ClickType.SHIFT_LEFT && click != ClickType.SHIFT_RIGHT) return;

                // Prevent default behavior and open our editor.
                event.setCancelled(true);
                module.openShulkerBox(player, clicked);
            }

            /**
             * Intercepts any click inside our virtual shulker view — prevents moving container item
             * and schedules an immediate commit after the click is processed.
             */
            @EventHandler
            public void onInventoryClickSync(InventoryClickEvent event) {
                Inventory inv = event.getInventory();
                InventoryHolder holder = inv.getHolder();
                if (!(holder instanceof ShulkerHolder shHolder)) return;

                // The viewer must be the owner (safety)
                if (!(event.getWhoClicked() instanceof Player player)) return;
                if (!player.getUniqueId().equals(shHolder.owner())) {
                    event.setCancelled(true);
                    return;
                }

                // If the click is in the player's inventory and targets the container item, block it.
                if (event.getClickedInventory() instanceof PlayerInventory) {
                    ItemStack target = player.getInventory().getItem(event.getSlot());
                    if (isMarkedWithSession(target, shHolder.sessionId())) {
                        // prevent the player from moving or manipulating the container item
                        event.setCancelled(true);
                        return;
                    }
                }

                // Also protect cases where the cursor or hotbar swap might involve the container item:
                ItemStack cursor = event.getCursor();
                if (isMarkedWithSession(cursor, shHolder.sessionId())) {
                    event.setCancelled(true);
                    return;
                }
                if (event.getHotbarButton() >= 0) {
                    ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                    if (isMarkedWithSession(hotbarItem, shHolder.sessionId())) {
                        // Prevent hotbar swap that would move the container item while open
                        event.setCancelled(true);
                        return;
                    }
                }

                // Allow the click to happen; schedule a commit on the next tick (0-tick delay) so the
                // click's changes have been applied, then write back into the container item immediately.
                module.plugin.getServer().getScheduler().runTask(module.plugin, () ->
                        commitViewToItem(player, shHolder, inv));
            }

            /**
             * Intercepts inventory drags affecting the view; protect container item and commit after changes.
             */
            @EventHandler
            public void onInventoryDrag(InventoryDragEvent event) {
                Inventory inv = event.getInventory();
                InventoryHolder holder = inv.getHolder();
                if (!(holder instanceof ShulkerHolder shHolder)) return;

                if (!(event.getWhoClicked() instanceof Player player)) return;
                if (!player.getUniqueId().equals(shHolder.owner())) {
                    event.setCancelled(true);
                    return;
                }

                // If any slot targeted by the drag is a player inventory slot containing the container, cancel.
                for (int rawSlot : event.getRawSlots()) {
                    // rawSlot >= top inventory size means it's player inventory
                    if (rawSlot >= inv.getSize()) {
                        int playerSlot = rawSlot - inv.getSize();
                        ItemStack target = player.getInventory().getItem(playerSlot);
                        if (isMarkedWithSession(target, shHolder.sessionId())) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }

                // schedule commit after drag processed
                module.plugin.getServer().getScheduler().runTask(module.plugin, () ->
                        commitViewToItem(player, shHolder, inv));
            }

            /**
             * When the view is closed remove the session mark and commit one final time.
             */
            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event) {
                InventoryHolder holder = event.getInventory().getHolder();
                if (!(holder instanceof ShulkerHolder shHolder)) return;

                Player player = Bukkit.getPlayer(shHolder.owner());
                if (player == null) {
                    // player offline; still try to find and clear the tag if possible by scanning online players is unnecessary.
                    // We'll still attempt to find the item in case it's available on the server (unlikely); skip for safety.
                    return;
                }

                // final commit (synchronous now)
                commitViewToItem(player, shHolder, event.getInventory());

                // find the container item in player's inventory by session and remove the session tag
                ItemStack item = findMarkedItemInInventory(player, shHolder.sessionId());
                if (item != null) {
                    // remove tag
                    if (item.getItemMeta() instanceof BlockStateMeta meta) {
                        try {
                            meta.getPersistentDataContainer().remove(SESSION);
                            item.setItemMeta(meta);
                            // write back into inventory
                            int idx = findItemIndex(player, shHolder.sessionId());
                            if (idx >= 0) player.getInventory().setItem(idx, item);
                            player.updateInventory();
                        } catch (Exception e) {
                            module.plugin.getLogger().warning("Failed to remove shulker session tag for player " + player.getName() + ": " + e.getMessage());
                        }
                    }
                }
                HOLDERS.remove(player);
            }

            @EventHandler
            public void onPlayerInteract(PlayerInteractEvent event) {
                if (event.hasBlock()) return;
                if (event.getItem() == null) return;
                if (isShulkerBox(event.getItem().getType())) {
                    ItemStack item = event.getItem();
                    event.setCancelled(true);
                    module.openShulkerBox(event.getPlayer(), item);
                }
            }

            /**
             * Commit the contents of the virtual inventory back into the marked container item.
             * This writes into the BlockStateMeta of the item found in the player's inventory that matches the session id.
             */
            private void commitViewToItem(Player player, ShulkerHolder holder, Inventory viewInv) {
                try {
                    ItemStack current = findMarkedItemInInventory(player, holder.sessionId());
                    if (current == null) {
                        // If we can't find the item, there's nothing safe we can do.
                        return;
                    }

                    if (!(current.getItemMeta() instanceof BlockStateMeta meta)) return;
                    if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

                    // set contents
                    shulker.getInventory().setContents(viewInv.getContents());
                    meta.setBlockState(shulker);
                    current.setItemMeta(meta);

                    // place the updated ItemStack back into the slot it occupies to force immediate sync.
                    int idx = findItemIndex(player, holder.sessionId());
                    if (idx >= 0) {
                        player.getInventory().setItem(idx, current);
                        player.updateInventory();
                    }
                } catch (Exception e) {
                    module.plugin.getLogger().warning("Failed to commit shulker contents for player " + player.getName() + ": " + e.getMessage());
                }
            }

            /**
             * Find an ItemStack in player's inventory that has the session tag equal to sessionId.
             * Returns the ItemStack reference or null if not found.
             */
            private ItemStack findMarkedItemInInventory(Player player, UUID sessionId) {
                int size = player.getInventory().getSize();
                for (int i = 0; i < size; i++) {
                    ItemStack it = player.getInventory().getItem(i);
                    if (isMarkedWithSession(it, sessionId)) return it;
                }
                // check offhand explicitly
                ItemStack off = player.getInventory().getItemInOffHand();
                if (isMarkedWithSession(off, sessionId)) return off;
                return null;
            }

            /**
             * Find index of item in player's inventory that has our session tag.
             * Returns slot index or -1.
             */
            private int findItemIndex(Player player, UUID sessionId) {
                int size = player.getInventory().getSize();
                for (int i = 0; i < size; i++) {
                    ItemStack it = player.getInventory().getItem(i);
                    if (isMarkedWithSession(it, sessionId)) return i;
                }
                // if offhand, return its index (in modern Bukkit API offhand is at last index: getSize()-1 often),
                // but since that's ambiguous across versions, return -1 and handle offhand separately if needed.
                ItemStack off = player.getInventory().getItemInOffHand();
                if (isMarkedWithSession(off, sessionId)) {
                    // try to place it into offhand slot index if required; many servers don't use numeric offhand index here.
                    return player.getInventory().getSize() - 1;
                }
                return -1;
            }

            private boolean isMarkedWithSession(ItemStack item, UUID sessionId) {
                if (item == null) return false;
                if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return false;
                String val = meta.getPersistentDataContainer().get(SESSION, PersistentDataType.STRING);
                if (val == null) return false;
                try {
                    UUID found = UUID.fromString(val);
                    return found.equals(sessionId);
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }

            private boolean isShulkerBox(Material material) {
                return material == Material.SHULKER_BOX ||
                        material == Material.BLACK_SHULKER_BOX ||
                        material == Material.BLUE_SHULKER_BOX ||
                        material == Material.BROWN_SHULKER_BOX ||
                        material == Material.CYAN_SHULKER_BOX ||
                        material == Material.GRAY_SHULKER_BOX ||
                        material == Material.GREEN_SHULKER_BOX ||
                        material == Material.LIGHT_BLUE_SHULKER_BOX ||
                        material == Material.LIGHT_GRAY_SHULKER_BOX ||
                        material == Material.LIME_SHULKER_BOX ||
                        material == Material.MAGENTA_SHULKER_BOX ||
                        material == Material.ORANGE_SHULKER_BOX ||
                        material == Material.PINK_SHULKER_BOX ||
                        material == Material.PURPLE_SHULKER_BOX ||
                        material == Material.RED_SHULKER_BOX ||
                        material == Material.WHITE_SHULKER_BOX ||
                        material == Material.YELLOW_SHULKER_BOX;
            }
        }
}
