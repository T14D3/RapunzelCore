package de.t14d3.rapunzelcore.modules.inventories.network;

/**
 * Small cross-server signal that an inventory profile was updated.
 * Carries no item data to keep plugin-messaging payloads small.
 */
public record InventorySyncPayload(
    String playerUuid,
    String contextKey,
    long updatedAt
) {
}
