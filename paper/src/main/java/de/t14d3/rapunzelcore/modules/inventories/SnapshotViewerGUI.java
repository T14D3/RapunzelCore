package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.InventoryProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for viewing inventory snapshots in a virtual chest.
 */
public class SnapshotViewerGUI implements Listener {
    private final RapunzelPaperCore paperCore;
    private final InventorySerializer serializer;
    private final Map<UUID, ViewerSession> sessions = new HashMap<>();

    public SnapshotViewerGUI(RapunzelPaperCore paperCore, InventorySerializer serializer) {
        this.paperCore = paperCore;
        this.serializer = serializer;
        Bukkit.getPluginManager().registerEvents(this, paperCore);
    }

    /**
     * Opens the snapshot viewer for a player.
     */
    public void openViewer(Player viewer, InventoryProfile snapshot) {
        // Create 54-slot inventory (6 rows)
        String title = "Snapshot: " + (snapshot.getSnapshotName().isEmpty() ? "#" + snapshot.getId() : snapshot.getSnapshotName());
        Inventory gui = Bukkit.createInventory(null, 54, Component.text(title, NamedTextColor.DARK_GREEN));

        // Decode inventory data
        ItemStack[] storage = serializer.decodeItems(new String(snapshot.getInventoryData()));
        ItemStack[] armor = serializer.decodeItems(new String(snapshot.getArmorData()));
        ItemStack[] extra = serializer.decodeItems(new String(snapshot.getExtraData()));

        // Fill storage contents (slots 0-35)
        for (int i = 0; i < Math.min(storage.length, 36); i++) {
            if (storage[i] != null) {
                gui.setItem(i, storage[i].clone());
            }
        }

        // Fill armor slots (slots 36-39) - helmet, chestplate, leggings, boots
        if (armor.length >= 4) {
            gui.setItem(39, armor[3] != null ? armor[3].clone() : null); // Helmet
            gui.setItem(38, armor[2] != null ? armor[2].clone() : null); // Chestplate
            gui.setItem(37, armor[1] != null ? armor[1].clone() : null); // Leggings
            gui.setItem(36, armor[0] != null ? armor[0].clone() : null); // Boots
        }

        // Offhand slot (slot 40)
        if (extra.length > 0 && extra[0] != null) {
            gui.setItem(40, extra[0].clone());
        }

        // Info book in slot 45
        gui.setItem(45, createInfoBook(snapshot));

        // Restore button in slot 49
        gui.setItem(49, createRestoreButton());

        // Ender chest button in slot 53
        gui.setItem(53, createEnderChestButton(snapshot));

        // Register session
        sessions.put(viewer.getUniqueId(), new ViewerSession(snapshot, gui));

        viewer.openInventory(gui);
    }

    /**
     * Opens the ender chest viewer for a snapshot.
     */
    public void openEnderChestViewer(Player viewer, InventoryProfile snapshot) {
        String title = "Ender Chest: " + (snapshot.getSnapshotName().isEmpty() ? "#" + snapshot.getId() : snapshot.getSnapshotName());
        Inventory gui = Bukkit.createInventory(null, 27, Component.text(title, NamedTextColor.DARK_PURPLE));

        // Decode ender chest data
        ItemStack[] enderChest = serializer.decodeEffects(new String(snapshot.getEnderChestData())) != null
                ? serializer.decodeItems(new String(snapshot.getEnderChestData()))
                : new ItemStack[0];

        // Fill ender chest contents
        if (enderChest != null) {
            for (int i = 0; i < Math.min(enderChest.length, 27); i++) {
                if (enderChest[i] != null) {
                    gui.setItem(i, enderChest[i].clone());
                }
            }
        }

        // Back button in slot 26
        gui.setItem(26, createBackButton());

        // Register session
        sessions.put(viewer.getUniqueId(), new ViewerSession(snapshot, gui, true));

        viewer.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ViewerSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        if (!event.getInventory().equals(session.inventory())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Handle restore button click
        if (!session.isEnderChest() && slot == 49) {
            player.closeInventory();
            player.performCommand("snapshot restore " + session.snapshot().getId());
            return;
        }

        // Handle ender chest button click
        if (!session.isEnderChest() && slot == 53) {
            Bukkit.getScheduler().runTask(paperCore, () -> openEnderChestViewer(player, session.snapshot()));
            return;
        }

        // Handle back button click
        if (session.isEnderChest() && slot == 26) {
            Bukkit.getScheduler().runTask(paperCore, () -> openViewer(player, session.snapshot()));
            return;
        }

        // Allow shift-click to copy items to player inventory
        if (event.isShiftClick() && slot < session.inventory().getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                event.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        sessions.remove(player.getUniqueId());
    }

    public void close() {
        HandlerList.unregisterAll(this);
        sessions.clear();
    }

    private ItemStack createInfoBook(InventoryProfile snapshot) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Snapshot Info", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("ID: ", NamedTextColor.GRAY).append(Component.text("#" + snapshot.getId(), NamedTextColor.WHITE)));
            lore.add(Component.text("Player: ", NamedTextColor.GRAY).append(Component.text(snapshot.getPlayerName(), NamedTextColor.WHITE)));
            lore.add(Component.text("Type: ", NamedTextColor.GRAY).append(Component.text(snapshot.getSnapshotType(), NamedTextColor.AQUA)));
            lore.add(Component.text("World: ", NamedTextColor.GRAY).append(Component.text(snapshot.getWorldGroup(), NamedTextColor.WHITE)));
            lore.add(Component.text("GameMode: ", NamedTextColor.GRAY).append(Component.text(snapshot.getGameMode(), NamedTextColor.WHITE)));
            lore.add(Component.text("Health: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f", snapshot.getHealth()), NamedTextColor.RED)));
            lore.add(Component.text("Food: ", NamedTextColor.GRAY).append(Component.text(snapshot.getFoodLevel(), NamedTextColor.GREEN)));
            lore.add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(snapshot.getExpLevel(), NamedTextColor.YELLOW)));
            lore.add(Component.empty());
            lore.add(Component.text("Created: ", NamedTextColor.GRAY).append(Component.text(new Date(snapshot.getCreatedAt()).toString(), NamedTextColor.WHITE)));
            if (!snapshot.getReason().isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Reason: ", NamedTextColor.GRAY).append(Component.text(snapshot.getReason(), NamedTextColor.WHITE)));
            }

            meta.lore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createRestoreButton() {
        ItemStack button = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Restore Snapshot", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to restore this snapshot", NamedTextColor.GRAY));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createEnderChestButton(InventoryProfile snapshot) {
        ItemStack button = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("View Ender Chest", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to view ender chest contents", NamedTextColor.GRAY));
            meta.lore(lore);
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back to Inventory", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));
            button.setItemMeta(meta);
        }
        return button;
    }

    private record ViewerSession(InventoryProfile snapshot, Inventory inventory, boolean isEnderChest) {
        ViewerSession(InventoryProfile snapshot, Inventory inventory) {
            this(snapshot, inventory, false);
        }
    }
}
