package de.t14d3.rapunzelcore.modules.inventories;

import java.util.Locale;
import java.util.Objects;

/**
 * Normalized inventory context key derived from a world-group and gamemode.
 */
public record InventoryContext(String worldGroup, String gameMode) {

    public InventoryContext {
        worldGroup = normalize(worldGroup);
        gameMode = normalize(gameMode);
    }

    public String key() {
        return worldGroup + ":" + gameMode;
    }

    public static String normalize(String raw) {
        if (raw == null) return "unknown";
        String normalized = raw.trim();
        if (normalized.isBlank()) return "unknown";
        return normalized.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryContext that)) return false;
        return Objects.equals(worldGroup, that.worldGroup)
            && Objects.equals(gameMode, that.gameMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldGroup, gameMode);
    }
}
