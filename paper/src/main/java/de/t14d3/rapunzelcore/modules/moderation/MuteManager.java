package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PlayerMute;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MuteManager {
    private final RapunzelCore core;
    private final SpoolDatabase database;
    private final Map<UUID, MuteData> activeMutes = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private MuteRepository muteRepository;

    public MuteManager(RapunzelCore core, SpoolDatabase database) {
        this.core = core;
        this.database = database;
        initDatabase();
        startCleanupTask();
    }

    private void initDatabase() {
        // Initialize repository - Spool auto-creates tables for entities
        this.muteRepository = new MuteRepository();
    }

    /**
     * Mutes a player
     */
    public void mutePlayer(UUID playerUuid, UUID mutedBy, String reason, long durationMs, String server) {
        long expiresAt = durationMs < 0 ? -1 : System.currentTimeMillis() + durationMs;
        long createdAt = System.currentTimeMillis();

        CoreDatabase.runLocked(() -> {
            try {
                // Remove any existing mute first
                PlayerMute existing = muteRepository.findByPlayerUuid(playerUuid.toString());
                if (existing != null) {
                    muteRepository.delete(existing);
                }

                // Create new mute
                PlayerMute mute = new PlayerMute();
                mute.setPlayerUuid(playerUuid.toString());
                mute.setMutedBy(mutedBy != null ? mutedBy.toString() : "CONSOLE");
                mute.setReason(reason);
                mute.setExpiresAt(expiresAt);
                mute.setCreatedAt(createdAt);
                mute.setServer(server);

                muteRepository.save(mute);
                CoreDatabase.getEntityManager().flush();

                // Update cache
                activeMutes.put(playerUuid, new MuteData(reason, expiresAt, mutedBy));
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to mute player: " + playerUuid, e);
            }
        });
    }

    /**
     * Unmutes a player
     */
    public void unmutePlayer(UUID playerUuid) {
        CoreDatabase.runLocked(() -> {
            try {
                PlayerMute existing = muteRepository.findByPlayerUuid(playerUuid.toString());
                if (existing != null) {
                    muteRepository.delete(existing);
                    CoreDatabase.getEntityManager().flush();
                }
                activeMutes.remove(playerUuid);
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to unmute player: " + playerUuid, e);
            }
        });
    }

    /**
     * Checks if a player is muted
     */
    public boolean isMuted(UUID playerUuid) {
        // Check cache first
        MuteData cached = activeMutes.get(playerUuid);
        if (cached != null) {
            if (cached.isExpired()) {
                activeMutes.remove(playerUuid);
                // Also clean up in DB
                CoreDatabase.runLockedAsync(() -> {
                    try {
                        PlayerMute existing = muteRepository.findByPlayerUuid(playerUuid.toString());
                        if (existing != null && existing.isExpired()) {
                            muteRepository.delete(existing);
                            CoreDatabase.getEntityManager().flush();
                        }
                    } catch (Exception e) {
                        RapunzelCore.getLogger().error("Failed to clean up expired mute", e);
                    }
                });
                return false;
            }
            return true;
        }

        // Check database
        return CoreDatabase.locked(() -> {
            try {
                PlayerMute mute = muteRepository.findByPlayerUuid(playerUuid.toString());
                if (mute != null && !mute.isExpired()) {
                    activeMutes.put(playerUuid, new MuteData(mute.getReason(), mute.getExpiresAt(), 
                        mute.getMutedBy().equals("CONSOLE") ? null : UUID.fromString(mute.getMutedBy())));
                    return true;
                }
                return false;
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to check mute status", e);
                return false;
            }
        });
    }

    /**
     * Gets mute info for a player
     */
    public MuteData getMuteInfo(UUID playerUuid) {
        // Check cache first
        MuteData cached = activeMutes.get(playerUuid);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        // Check database
        return CoreDatabase.locked(() -> {
            try {
                PlayerMute mute = muteRepository.findByPlayerUuid(playerUuid.toString());
                if (mute != null && !mute.isExpired()) {
                    MuteData data = new MuteData(mute.getReason(), mute.getExpiresAt(),
                        mute.getMutedBy().equals("CONSOLE") ? null : UUID.fromString(mute.getMutedBy()));
                    activeMutes.put(playerUuid, data);
                    return data;
                }
                return null;
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to get mute info", e);
                return null;
            }
        });
    }

    /**
     * Loads mute data for a player
     */
    public void loadPlayerData(UUID playerUuid) {
        if (loadedPlayers.contains(playerUuid)) {
            return;
        }
        loadedPlayers.add(playerUuid);

        CoreDatabase.runLockedAsync(() -> {
            try {
                PlayerMute mute = muteRepository.findByPlayerUuid(playerUuid.toString());
                if (mute != null && !mute.isExpired()) {
                    activeMutes.put(playerUuid, new MuteData(mute.getReason(), mute.getExpiresAt(),
                        mute.getMutedBy().equals("CONSOLE") ? null : UUID.fromString(mute.getMutedBy())));
                }
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to load mute data for: " + playerUuid, e);
            }
        });
    }

    /**
     * Unloads mute data for a player
     */
    public void unloadPlayerData(UUID playerUuid) {
        loadedPlayers.remove(playerUuid);
        activeMutes.remove(playerUuid);
    }

    /**
     * Gets all active mutes
     */
    public Map<UUID, MuteData> getActiveMutes() {
        return new HashMap<>(activeMutes);
    }

    /**
     * Clears expired mutes from cache and database
     */
    public void clearExpiredMutes() {
        // Clear from cache
        activeMutes.entrySet().removeIf(entry -> entry.getValue().isExpired());

        // Clear from database
        CoreDatabase.runLockedAsync(() -> {
            try {
                List<PlayerMute> allMutes = muteRepository.findAll();
                for (PlayerMute mute : allMutes) {
                    if (mute.isExpired()) {
                        muteRepository.delete(mute);
                    }
                }
                CoreDatabase.getEntityManager().flush();
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to clear expired mutes", e);
            }
        });
    }

        /**
     * Cleans up resources when the module is disabled.
     */
    public void cleanup() {
        // Cancel the cleanup task
        // Note: The task is scheduled asynchronously and will be cancelled when plugin disables

        // Clear all cached data
        activeMutes.clear();
        loadedPlayers.clear();

        RapunzelCore.getLogger().info("MuteManager cleaned up.");
    }

    private void startCleanupTask() {
        // Use RapunzelPaperCore.getInstance() to get the JavaPlugin
        Bukkit.getScheduler().runTaskTimerAsynchronously(RapunzelPaperCore.getInstance(), 
            this::clearExpiredMutes, 20L * 60 * 5, 20L * 60 * 5); // Run every 5 minutes
    }

    /**
     * Repository for PlayerMute entity operations
     */
    private class MuteRepository extends EntityRepository<PlayerMute> {
        MuteRepository() {
            super(CoreDatabase.getEntityManager(), PlayerMute.class);
        }

        PlayerMute findByPlayerUuid(String playerUuid) {
            return findAll().stream()
                .filter(m -> m.getPlayerUuid().equals(playerUuid))
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Data class for mute information
     */
    public static class MuteData {
        private final String reason;
        private final long expiresAt;
        private final UUID mutedBy;

        public MuteData(String reason, long expiresAt, UUID mutedBy) {
            this.reason = reason;
            this.expiresAt = expiresAt;
            this.mutedBy = mutedBy;
        }

        public String getReason() {
            return reason;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public UUID getMutedBy() {
            return mutedBy;
        }

        public boolean isExpired() {
            return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
        }

        public boolean isPermanent() {
            return expiresAt < 0;
        }
    }
}
