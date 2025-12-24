package de.t14d3.rapunzelcore.modules.commands.invsee;

import de.t14d3.rapunzelcore.RapunzelCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages a mirrored view of a target player's inventory for a viewer.
 * This class creates a custom inventory GUI that reflects the target's
 * inventory (including main inventory, hotbar, armor, and offhand) in real-time.
 */
public class PlayerInventoryMirror {

    // A map to store active mirrors, mapping viewer UUID to the mirror instance.
    // This allows the central MirrorListener to quickly find the mirror associated with a viewer.
    public static final Map<UUID, PlayerInventoryMirror> activeMirrors = new HashMap<>();

    public final Player viewer;
    public final Player target;
    private final Inventory mirroredInventory;
    private BukkitTask syncTask; // Task for periodic inventory synchronization

    /**
     * Constructor for PlayerInventoryMirror.
     *
     * @param viewer The player who is viewing the target's inventory.
     * @param target The player whose inventory is being viewed.
     */
    public PlayerInventoryMirror(Player viewer, Player target) {
        this.viewer = viewer;
        this.target = target;
        // Create a 54-slot chest inventory for the mirror.
        // This size accommodates all player inventory slots plus some extra for layout.
        this.mirroredInventory = Bukkit.createInventory(null, 54, target.getName() + "'s Inventory");

        // Store the active mirror in the static map.
        activeMirrors.put(viewer.getUniqueId(), this);

        // Synchronize inventories initially and start a periodic sync task.
        syncInventories();
        startSyncTask();
    }

    /**
     * Initializes the mirrored inventory with the current contents of the target's inventory.
     * This method also sets up the layout for main inventory, hotbar, armor, and offhand.
     *
     * Layout in 54-slot chest:
     * Slots 0-8: Target's Hotbar (PlayerInventory slots 0-8)
     * Slots 9-35: Target's RapunzelPaperCore Inventory (PlayerInventory slots 9-35)
     * Slots 38-39 & 47-48: Armor Slots (Helmet, Chestplate, Leggings, Boots - PlayerInventory slots 39-36)
     * Slot 41: Offhand Slot (PlayerInventory slot 40)
     * Slot 50: Cursor Item
     */
    private void syncInventories() {
        PlayerInventory targetInv = target.getInventory();

        // Clear the mirrored inventory first to ensure clean state
        mirroredInventory.clear();

        // Copy main inventory and hotbar (slots 0-35)
        for (int i = 0; i < 36; i++) {
            mirroredInventory.setItem(i, targetInv.getItem(i));
        }

        // Fill bottom two rows with glass panes
        for (int i = 36; i < 54; i++) {
            mirroredInventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        // Copy armor contents
        mirroredInventory.setItem(38, targetInv.getHelmet());    // Display Slot for Helmet
        mirroredInventory.setItem(47, targetInv.getChestplate());// Display Slot for Chestplate
        mirroredInventory.setItem(39, targetInv.getLeggings());  // Display Slot for Leggings
        mirroredInventory.setItem(48, targetInv.getBoots());     // Display Slot for Boots

        // Copy offhand item
        mirroredInventory.setItem(41, targetInv.getItemInOffHand()); // Display Slot for Offhand
        mirroredInventory.setItem(50, targetInv.getHolder().getItemOnCursor()); // Display Slot for Cursor item
    }

    /**
     * Starts a periodic task to synchronize the mirrored inventory with the target's actual inventory.
     * This helps ensure that changes made by the target player are reflected live for the viewer.
     */
    private void startSyncTask() {
        // Run every 5 ticks (0.25 seconds)
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!viewer.isOnline() || !target.isOnline()) {
                    // If either player is offline, stop the mirror and clean up.
                    closeMirror(viewer, target);
                    return;
                }
                syncInventories();
                // If a player has a different inventory open, close it (e.g., if they moved away from the InvSee GUI)
                if (viewer.getOpenInventory().getTopInventory() != mirroredInventory) {
                    closeMirror(viewer, target);
                }
            }
        }.runTaskTimer((Plugin) RapunzelCore.getInstance(), 5L, 5L); // Initial delay of 5 ticks, then repeat every 5 ticks
    }

    /**
     * Opens the mirrored inventory for the viewer.
     */
    public void open() {
        viewer.openInventory(mirroredInventory);
    }

    /**
     * Gets the custom mirrored inventory instance.
     * @return The Inventory being displayed as a mirror.
     */
    public Inventory getMirroredInventory() {
        return mirroredInventory;
    }

    /**
     * Handles inventory click events for this specific mirrored inventory.
     * This method is called by the central MirrorListener.
     * @param event The InventoryClickEvent.
     */
    @SuppressWarnings("deprecation")
    public void handleInventoryClick(InventoryClickEvent event) {
        // Always cancel the event to manually handle item movements and prevent desync/duplication.
        event.setCancelled(true);
        if (!event.getWhoClicked().hasPermission("rapunzelcore.invsee.modify")) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem(); // Item in the slot that was clicked
        ItemStack cursorItem = event.getCursor();       // Item held by the cursor

        PlayerInventory targetInv = this.target.getInventory();
        PlayerInventory viewerInv = this.viewer.getInventory();

        int clickedSlot = event.getRawSlot(); // Raw slot including the bottom inventory (player's own inv)

        boolean clickedInMirroredInventory = event.getClickedInventory() != null &&
                event.getClickedInventory().equals(this.mirroredInventory);

        if (clickedInMirroredInventory) {
            // Map the clicked display slot back to the target's actual inventory slot
            int targetSlot = getTargetInventorySlot(clickedSlot);

            if (targetSlot == -1) { // Clicked an empty/decorative slot in the mirror
                return;
            }

            if (event.getAction() == InventoryAction.PICKUP_ALL ||
                    event.getAction() == InventoryAction.PICKUP_HALF ||
                    event.getAction() == InventoryAction.PICKUP_ONE ||
                    event.getAction() == InventoryAction.PICKUP_SOME) {
                // Picking up item from mirrored inventory to cursor
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    this.mirroredInventory.setItem(clickedSlot, null); // Remove from mirror immediately
                    updateTargetInventoryItem(targetInv, targetSlot, null); // Remove from target's actual inventory
                    event.setCursor(clickedItem); // Place on viewer's cursor
                }

            } else if (event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME) {
                // Placing item from cursor to mirrored inventory slot
                if (cursorItem.getType() != Material.AIR) {
                    ItemStack newCursorItem = cursorItem.clone(); // Clone to prevent modification issues
                    int amountToPlace = event.getAction() == InventoryAction.PLACE_ONE ? 1 : newCursorItem.getAmount();

                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        // If slot is not empty, try to stack or swap
                        if (clickedItem.isSimilar(newCursorItem) && clickedItem.getAmount() < clickedItem.getMaxStackSize()) {
                            // Stack items
                            int space = clickedItem.getMaxStackSize() - clickedItem.getAmount();
                            int transferAmount = Math.min(amountToPlace, space);
                            clickedItem.setAmount(clickedItem.getAmount() + transferAmount);
                            newCursorItem.setAmount(newCursorItem.getAmount() - transferAmount);
                            // Update target's inventory for stacking
                            updateTargetInventoryItem(targetInv, targetSlot, clickedItem);
                            event.setCursor(newCursorItem.getAmount() > 0 ? newCursorItem : null);
                        } else {
                            // Swap items
                            this.mirroredInventory.setItem(clickedSlot, newCursorItem);
                            updateTargetInventoryItem(targetInv, targetSlot, newCursorItem);
                            event.setCursor(clickedItem);
                        }
                    } else {
                        // Place into empty slot
                        this.mirroredInventory.setItem(clickedSlot, newCursorItem);
                        updateTargetInventoryItem(targetInv, targetSlot, newCursorItem);
                        event.setCursor(null); // Cursor becomes empty
                    }
                }

            } else if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                // Swap item from slot with item on cursor
                ItemStack oldSlotItem = clickedItem != null ? clickedItem.clone() : null;
                ItemStack newSlotItem = cursorItem.clone();

                this.mirroredInventory.setItem(clickedSlot, newSlotItem);
                updateTargetInventoryItem(targetInv, targetSlot, newSlotItem);
                event.setCursor(oldSlotItem);

            } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Shift-clicking from mirrored inventory to viewer's inventory
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    // Attempt to add to viewer's inventory
                    HashMap<Integer, ItemStack> remaining = viewerInv.addItem(clickedItem.clone());
                    if (remaining.isEmpty()) {
                        // All items moved successfully
                        this.mirroredInventory.setItem(clickedSlot, null);
                        updateTargetInventoryItem(targetInv, targetSlot, null);
                    } else {
                        // Some items remained, update the original stack size in mirror and target
                        ItemStack remainingItem = remaining.get(0);
                        int amountMoved = clickedItem.getAmount() - remainingItem.getAmount();
                        if (amountMoved > 0) {
                            clickedItem.setAmount(remainingItem.getAmount());
                            this.mirroredInventory.setItem(clickedSlot, clickedItem);
                            updateTargetInventoryItem(targetInv, targetSlot, clickedItem);
                        }
                    }
                }
            }
            // After any change, manually resync the mirrored inventory to reflect target's state
            syncInventories();

        } else if (event.getClickedInventory() != null && event.getClickedInventory().equals(this.viewer.getInventory())) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    ItemStack itemToMove = clickedItem.clone();

                    // Attempt to add the item to the target's inventory
                    HashMap<Integer, ItemStack> remainingFromTargetAdd = targetInv.addItem(itemToMove);

                    if (remainingFromTargetAdd.isEmpty()) {
                        // All items successfully moved to target.
                        // So, the viewer's original slot should become empty.
                        viewerInv.setItem(event.getSlot(), null);
                    } else {
                        // Not all items could be moved.
                        // Calculate how many were successfully moved.
                        int movedAmount = itemToMove.getAmount() - remainingFromTargetAdd.get(0).getAmount();

                        if (movedAmount > 0) {
                            // Some items were moved, reduce the stack in the viewer's inventory.
                            clickedItem.setAmount(clickedItem.getAmount() - movedAmount);
                            viewerInv.setItem(event.getSlot(), clickedItem);
                        }
                        // If movedAmount is 0, nothing was moved, so the viewer's slot remains as is.
                    }
                    viewer.updateInventory();
                }
                syncInventories();
            }
        }
    }

    public void handleInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true); // Always cancel drag events to take control

        if (!event.getWhoClicked().hasPermission("rapunzelcore.invsee.modify")) {
            return;
        }

        ItemStack draggedItem = event.getOldCursor(); // The item that was on the cursor before the drag started.
        if (draggedItem.getType() == Material.AIR) {
            // Nothing to drag, or item disappeared unexpectedly.
            viewer.updateInventory(); // Ensure client is updated
            syncInventories(); // Ensure mirror is updated
            return;
        }

        PlayerInventory targetInv = this.target.getInventory();
        PlayerInventory viewerInv = this.viewer.getInventory();

        // Iterate through all slots where items would be placed by the drag operation.
        // event.getNewItems() maps raw slot -> ItemStack that would be placed there.
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            int rawSlot = entry.getKey(); // The raw slot index where an item part would go
            ItemStack itemToPlace = entry.getValue(); // The item stack that would be placed in this slot

            // Determine if the slot is in the mirrored inventory (top) or viewer's inventory (bottom)
            if (rawSlot >= 0 && rawSlot < mirroredInventory.getSize()) { // Top inventory (mirrored)
                int targetSlot = getTargetInventorySlot(rawSlot);
                if (targetSlot != -1) {
                    this.mirroredInventory.setItem(rawSlot, itemToPlace);
                    updateTargetInventoryItem(targetInv, targetSlot, itemToPlace);
                }
            } else {
                int viewerSlot = rawSlot - mirroredInventory.getSize(); // Adjust raw slot to viewer's inventory index

                // Only apply if the calculated viewerSlot is within the bounds of a standard PlayerInventory
                if (viewerSlot >= 0 && viewerSlot < viewerInv.getSize()) { // Use viewerInv.getSize() for safety (typically 40)
                    viewerInv.setItem(viewerSlot, itemToPlace); // Update viewer's actual inventory on the server
                }
            }
        }
        event.getWhoClicked().setItemOnCursor(null);
        viewer.updateInventory();
        syncInventories();
    }

    public void handleInventoryClose(InventoryCloseEvent event) {
        // This is our mirrored inventory being closed by the viewer.
        closeMirror(this.viewer, this.target);
    }

    public void handlePlayerQuit(PlayerQuitEvent event) {
        // If the quitting player is the viewer of this mirror
        if (this.viewer.equals(event.getPlayer())) {
            closeMirror(this.viewer, this.target);
        }
        // If the quitting player is the target of this mirror
        else if (this.target.equals(event.getPlayer())) {
            closeMirror(this.viewer, this.target);
            // Notify viewer if they are still online
            if (this.viewer.isOnline()) {
                this.viewer.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.invsee.error.target_offline", this.target.getName()));
            }
        }
    }

    private int getTargetInventorySlot(int displaySlot) {
        if (displaySlot >= 0 && displaySlot < 36) {
            // RapunzelPaperCore inventory and hotbar (0-35) map directly
            return displaySlot;
        } else {
            // Map new armor grid and offhand positions
            switch (displaySlot) {
                case 38: return 38; // Helmet
                case 47: return 39; // Chestplate
                case 39: return 40; // Leggings
                case 48: return 41; // Boots
                case 41: return 42; // Offhand
                case 50: return 43; // Cursor
                default: return -1; // Unmapped slot
            }
        }
    }

    /**
     * Updates an item in the target's actual inventory based on the given slot.
     * This handles mapping back to armor/offhand methods for PlayerInventory.
     * @param targetInv The target's PlayerInventory.
     * @param targetSlot The actual target PlayerInventory slot.
     * @param item The ItemStack to set.
     */
    private void updateTargetInventoryItem(PlayerInventory targetInv, int targetSlot, ItemStack item) {
        if (targetSlot >= 0 && targetSlot < 36) {
            targetInv.setItem(targetSlot, item);
        } else {
            // Special handling for armor and offhand slots due to PlayerInventory methods
            switch (targetSlot) {
                case 38: // Helmet
                    targetInv.setHelmet(item);
                    break;
                case 39: // Chestplate
                    targetInv.setChestplate(item);
                    break;
                case 40: // Leggings
                    targetInv.setLeggings(item);
                    break;
                case 41: // Boots
                    targetInv.setBoots(item);
                    break;
                case 42: // Offhand
                    targetInv.setItemInOffHand(item);
                    break;
                case 43: // Cursor
                    targetInv.getHolder().setItemOnCursor(item);
                    break;
                default:
                    // Should not happen for mapped slots, but for safety
                    break;
            }
        }
    }

    private void closeMirror(Player viewerPlayer, Player targetPlayer) {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel();
        }
        activeMirrors.remove(viewerPlayer.getUniqueId());
        viewerPlayer.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.invsee.closed", targetPlayer.getName()));
    }

    /**
     * Static method to get an active mirror instance by viewer UUID.
     * @param viewerUUID The UUID of the viewer.
     * @return The PlayerInventoryMirror instance, or null if not found.
     */
    public static PlayerInventoryMirror getActiveMirror(UUID viewerUUID) {
        return activeMirrors.get(viewerUUID);
    }
}
