package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzelcore.RapunzelCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Automatic snapshot task that creates AUTO snapshots for online players.
 */
public class AutoSnapshotTask implements Runnable {
    private final InventoryRepository repository;
    private final InventorySerializer serializer;
    private final InventoryConfig config;
    private final ConcurrentMap<UUID, Long> lastSnapshotTime = new ConcurrentHashMap<>();

    AutoSnapshotTask(InventoryConfig config, InventorySerializer serializer) {
        this.repository = InventoryRepository.getInstance();
        this.serializer = serializer;
        this.config = config;
    }

    @Override
    public void run() {
        if (!config.snapshotsEnabled || !config.autoSnapshotEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMillis = config.autoSnapshotIntervalMinutes * 60L * 1000L;

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            Long lastTime = lastSnapshotTime.get(playerId);

            // Check if enough time has passed since last snapshot
            if (lastTime != null && (now - lastTime) < intervalMillis) {
                continue;
            }

            // Create auto snapshot using centralized repository method
            String snapshotName = "Auto-" + System.currentTimeMillis();
            repository.createSnapshotAsync(player, snapshotName, "AUTO", "SYSTEM", "Automatic snapshot", serializer, config).thenAccept(profile -> {
                if (profile != null) {
                    RapunzelCore.getLogger().debug("Created auto snapshot for player: {}", player.getName());
                }
            }).exceptionally(error -> {
                RapunzelCore.getLogger().error("Failed to create auto snapshot for player: {}", player.getName(), error);
                return null;
            });

            lastSnapshotTime.put(playerId, now);

            // Prune old snapshots for this player
            prunePlayerSnapshots(playerId);
        }
    }

    /**
     * Prunes old snapshots for a player.
     */
    private void prunePlayerSnapshots(UUID playerUuid) {
        long maxAgeMillis = config.retentionDays * 24L * 60L * 60L * 1000L;
        repository.pruneSnapshotsAsync(playerUuid, config.maxSnapshotsPerPlayer, maxAgeMillis).thenAccept(deleted -> {
            if (deleted > 0) {
                RapunzelCore.getLogger().debug("Pruned {} old snapshots for player: {}", deleted, playerUuid);
            }
        });
    }

    /**
     * Cleans up tracking data for a player who has logged out.
     */
    public void removePlayer(UUID playerId) {
        lastSnapshotTime.remove(playerId);
    }
}
