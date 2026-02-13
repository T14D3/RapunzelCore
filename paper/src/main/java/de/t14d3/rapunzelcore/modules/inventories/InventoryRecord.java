package de.t14d3.rapunzelcore.modules.inventories;

/**
 * Serialized inventory payload stored in the database or sent across the network.
 * Strings contain encoded item stacks (base64/gzip handled by platform layer).
 */
public record InventoryRecord(
    InventoryContext context,
    String inventoryData,
    String armorData,
    String extraData,
    String enderChestData,
    String potionData,
    double health,
    int foodLevel,
    float saturation,
    int expLevel,
    float expProgress,
    int heldSlot,
    long updatedAt
) {
    public String contextKey() {
        return context == null ? null : context.key();
    }

    public String worldGroup() {
        return context == null ? null : context.worldGroup();
    }

    public String gameMode() {
        return context == null ? null : context.gameMode();
    }
}
