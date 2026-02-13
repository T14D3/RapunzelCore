package de.t14d3.rapunzelcore.modules.inventories;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runtime snapshot of a player's inventory and basic state.
 */
public record InventorySnapshot(
    ItemStack[] storage,
    ItemStack[] armor,
    ItemStack[] extra,
    ItemStack[] enderChest,
    List<PotionEffect> effects,
    double health,
    int foodLevel,
    float saturation,
    int expLevel,
    float expProgress,
    int heldSlot
) {
    public static InventorySnapshot emptyFor(Player player) {
        ItemStack[] storage = new ItemStack[player.getInventory().getStorageContents().length];
        ItemStack[] armor = new ItemStack[player.getInventory().getArmorContents().length];
        ItemStack[] extra = new ItemStack[player.getInventory().getExtraContents().length];
        ItemStack[] ender = new ItemStack[player.getEnderChest().getContents().length];

        double maxHealth = player.getMaxHealth();

        return new InventorySnapshot(
            storage,
            armor,
            extra,
            ender,
            List.of(),
            maxHealth,
            20,
            5.0f,
            0,
            0.0f,
            player.getInventory().getHeldItemSlot()
        );
    }

    public InventorySnapshot withEffects(Collection<PotionEffect> newEffects) {
        List<PotionEffect> copy = new ArrayList<>();
        if (newEffects != null) copy.addAll(newEffects);
        return new InventorySnapshot(
            storage,
            armor,
            extra,
            enderChest,
            copy,
            health,
            foodLevel,
            saturation,
            expLevel,
            expProgress,
            heldSlot
        );
    }
}
