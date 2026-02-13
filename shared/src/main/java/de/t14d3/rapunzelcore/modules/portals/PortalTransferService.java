package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for handling portal transfers and cross-server entity movement.
 * Extends EntityTransferService to provide portal-specific transfer capabilities.
 */
public interface PortalTransferService extends PortalService, EntityTransferService {

    /**
     * Transfers an entity through a portal.
     * If the portal targets another server, initiates a cross-server transfer.
     * Otherwise, performs a local teleportation.
     *
     * @param entityUuid the UUID of the entity to transfer
     * @param portal the portal to transfer through
     * @return CompletableFuture that completes with true if transfer was successful
     */
    CompletableFuture<Boolean> transferThroughPortal(@NotNull UUID entityUuid, @NotNull Portal portal);

    /**
     * Handles an entity entering a portal.
     * This method checks cooldowns, fires the PortalEntryEvent, and executes the appropriate action.
     *
     * @param entityUuid the UUID of the entity entering the portal
     * @param portal the portal being entered
     */
    void handlePortalEntry(@NotNull UUID entityUuid, @NotNull Portal portal);

    /**
     * Registers a portal with the transfer service.
     * Registered portals can be found via findPortalAt() and will have particles spawned.
     *
     * @param portal the portal to register
     */
    void registerPortal(@NotNull Portal portal);

    /**
     * Unregisters a portal from the transfer service.
     *
     * @param portalId the UUID of the portal to unregister
     */
    void unregisterPortal(@NotNull UUID portalId);

    /**
     * Finds a registered portal at the specified location.
     *
     * @param world the world name
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return Optional containing the portal if found at this location
     */
    Optional<Portal> findPortalAt(@NotNull String world, double x, double y, double z);

    /**
     * Spawns particle effects for a portal.
     * Implementation should check if particles are enabled in the portal's ParticleConfig.
     *
     * @param portal the portal to spawn particles for
     */
    void spawnPortalParticles(@NotNull Portal portal);

    /**
     * Gets the default cooldown duration in milliseconds between portal uses for an entity.
     *
     * @return the cooldown duration in milliseconds
     */
    default long getPortalCooldownMillis() {
        return 1000L; // 1 second default
    }

    /**
     * Checks if an entity is currently on cooldown for portal usage.
     *
     * @param entityUuid the entity UUID to check
     * @return true if the entity is on cooldown
     */
    boolean isOnCooldown(@NotNull UUID entityUuid);

    /**
     * Gets the remaining cooldown time in milliseconds for an entity.
     *
     * @param entityUuid the entity UUID
     * @return remaining cooldown in milliseconds, or 0 if not on cooldown
     */
    long getRemainingCooldown(@NotNull UUID entityUuid);
}
