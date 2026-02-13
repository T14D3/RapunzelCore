package de.t14d3.rapunzelcore.modules.portals;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing portals and their operations.
 */
public interface PortalService {

    /**
     * Creates a new portal.
     *
     * @param portal the portal to create
     * @return a future that completes when the portal is created
     */
    CompletableFuture<Portal> createPortal(@NotNull Portal portal);

    /**
     * Updates an existing portal.
     *
     * @param portal the portal to update
     * @return a future that completes when the portal is updated
     */
    CompletableFuture<Portal> updatePortal(@NotNull Portal portal);

    /**
     * Deletes a portal by its ID.
     *
     * @param portalId the portal ID
     * @return a future that completes when the portal is deleted
     */
    CompletableFuture<Boolean> deletePortal(@NotNull UUID portalId);

    /**
     * Gets a portal by its ID.
     *
     * @param portalId the portal ID
     * @return an optional containing the portal if found
     */
    Optional<Portal> getPortal(@NotNull UUID portalId);

    /**
     * Gets a portal by its name.
     *
     * @param name the portal name
     * @return an optional containing the portal if found
     */
    Optional<Portal> getPortalByName(@NotNull String name);

    /**
     * Gets all portals.
     *
     * @return a collection of all portals
     */
    Collection<Portal> getAllPortals();

    /**
     * Gets all portals in a specific world.
     *
     * @param world the world name
     * @return a collection of portals in the world
     */
    Collection<Portal> getPortalsInWorld(@NotNull String world);

    /**
     * Finds a portal that contains the given location.
     *
     * @param world the world name
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return an optional containing the portal if found
     */
    Optional<Portal> findPortalAt(@NotNull String world, double x, double y, double z);

    /**
     * Enables a portal.
     *
     * @param portalId the portal ID
     * @return true if the portal was enabled
     */
    CompletableFuture<Boolean> enablePortal(@NotNull UUID portalId);

    /**
     * Disables a portal.
     *
     * @param portalId the portal ID
     * @return true if the portal was disabled
     */
    CompletableFuture<Boolean> disablePortal(@NotNull UUID portalId);

    /**
     * Checks if a player has permission to use a portal.
     *
     * @param portal the portal
     * @param playerId the player UUID
     * @return true if the player can use the portal
     */
    boolean canUsePortal(@NotNull Portal portal, @NotNull UUID playerId);

    /**
     * Processes an entity entering a portal.
     *
     * @param entityId the entity UUID
     * @param portal the portal entered
     * @return a future that completes when processing is done
     */
    CompletableFuture<Void> processPortalEntry(@NotNull UUID entityId, @NotNull Portal portal);

    /**
     * Registers a portal action handler.
     *
     * @param actionType the action type
     * @param handler the handler
     */
    void registerActionHandler(@NotNull String actionType, @NotNull PortalActionHandler handler);

    /**
     * Unregisters a portal action handler.
     *
     * @param actionType the action type
     */
    void unregisterActionHandler(@NotNull String actionType);

    /**
     * Gets the local server name.
     *
     * @return the server name
     */
    @NotNull
    String getLocalServerName();

    /**
     * Checks if cross-server portals are enabled.
     *
     * @return true if cross-server is enabled
     */
    boolean isCrossServerEnabled();

    /**
     * Reloads all portals from storage.
     *
     * @return a future that completes when reload is done
     */
    CompletableFuture<Void> reloadPortals();

    /**
     * Shuts down the portal service.
     */
    void shutdown();

    /**
     * Handler for custom portal actions.
     */
    @FunctionalInterface
    interface PortalActionHandler {
        /**
         * Handles a portal action.
         *
         * @param entityId the entity that triggered the action
         * @param portal the portal
         * @param action the action string
         * @return true if the action was handled
         */
        boolean handle(@NotNull UUID entityId, @NotNull Portal portal, @NotNull String action);
    }
}
