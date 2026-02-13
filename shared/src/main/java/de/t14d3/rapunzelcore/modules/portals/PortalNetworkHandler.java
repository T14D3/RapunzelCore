package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.network.NetworkManager;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzelcore.modules.portals.network.PortalPayload;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles network communication for portals across the server network.
 * Manages portal synchronization, entry notifications, and cross-server
 * entity transfers through portals.
 */
public class PortalNetworkHandler {

    private final Logger logger;
    private final NetworkEventBus eventBus;
    private final PortalTransferService portalTransferService;
    private final String localServerName;
    private NetworkEventBus.Subscription subscription;

    /**
     * Creates a new PortalNetworkHandler.
     *
     * @param logger the logger instance
     * @param messenger the messenger for network communication
     * @param portalTransferService the portal transfer service for handling transfers
     * @param localServerName the name of this server in the network
     */
    public PortalNetworkHandler(
        @NotNull Logger logger,
        @NotNull Messenger messenger,
        @NotNull PortalTransferService portalTransferService,
        @NotNull String localServerName
    ) {
        this.logger = logger;
        this.eventBus = new NetworkEventBus(messenger);
        this.portalTransferService = portalTransferService;
        this.localServerName = localServerName;
    }

    /**
     * Registers this handler with the network event bus to receive portal messages.
     */
    public void register() {
        if (subscription != null) {
            logger.warning("PortalNetworkHandler is already registered");
            return;
        }

        // Register for all portal message types
        subscription = eventBus.register(NetworkChannels.PORTALS, Object.class, this::handleMessage);
        logger.info("PortalNetworkHandler registered for channel: " + NetworkChannels.PORTALS);
    }

    /**
     * Unregisters this handler from the network event bus.
     */
    public void unregister() {
        if (subscription == null) {
            return;
        }

        subscription.close();
        subscription = null;
        logger.info("PortalNetworkHandler unregistered");
    }

    /**
     * Handles incoming portal messages from the network.
     *
     * @param message the received message
     * @param sender the server that sent the message
     */
    private void handleMessage(@NotNull Object message, @NotNull String sender) {
        try {
            switch (message) {
                case PortalEntryNotification notification -> handleEntryNotification(notification, sender);
                case PortalUpdateMessage update -> handlePortalUpdate(update, sender);
                case PortalSyncRequest syncRequest -> handleSyncRequest(syncRequest, sender);
                case PortalPayload payload -> handlePortalPayload(payload, sender);
                default -> logger.warning("Received unknown portal message type: " + message.getClass().getName());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling portal message from " + sender, e);
        }
    }

    /**
     * Handles a portal entry notification from another server.
     * This is called when an entity enters a portal on another server
     * that targets this server.
     *
     * @param notification the entry notification
     * @param sender the source server
     */
    private void handleEntryNotification(@NotNull PortalEntryNotification notification, @NotNull String sender) {
        logger.info("Received portal entry notification from " + sender +
                   ": entity " + notification.entityUuid() +
                   " via portal " + notification.portalId());

        // The entity should be arriving soon via the entity transfer system
        // We can prepare any portal-specific setup here
        if (!localServerName.equals(notification.targetServer())) {
            logger.warning("Received entry notification for wrong server. Expected: " +
                          notification.targetServer() + ", got: " + localServerName);
            return;
        }

        // Pre-register the incoming entity for any portal-specific handling
        // The actual entity arrival will be handled by the EntityTransferService
    }

    /**
     * Handles a portal update message from another server.
     *
     * @param update the portal update
     * @param sender the source server
     */
    private void handlePortalUpdate(@NotNull PortalUpdateMessage update, @NotNull String sender) {
        logger.fine("Received portal update from " + sender + ": " + update.portalId());

        // Update local portal registry if this is a shared/network portal
        Portal portal = update.portal();
        if (portal != null && portal.isCrossServer()) {
            portalTransferService.registerPortal(portal);
        }
    }

    /**
     * Handles a portal sync request from another server.
     *
     * @param request the sync request
     * @param sender the requesting server
     */
    private void handleSyncRequest(@NotNull PortalSyncRequest request, @NotNull String sender) {
        logger.fine("Received portal sync request from " + sender);

        // Send back our network portals
        portalTransferService.findPortalAt(request.world(), request.x(), request.y(), request.z())
            .ifPresent(portal -> {
                if (portal.isCrossServer()) {
                    sendPortalUpdate(portal, sender);
                }
            });
    }

    /**
     * Handles a portal payload message.
     *
     * @param payload the portal payload
     * @param sender the source server
     */
    private void handlePortalPayload(@NotNull PortalPayload payload, @NotNull String sender) {
        logger.fine("Received portal payload from " + sender + ": " + payload.id());

        // Handle any portal-specific payload data
        // This could include particle sync, state changes, etc.
    }

    /**
     * Sends a portal entry notification to the target server.
     * This should be called when an entity enters a portal targeting another server.
     *
     * @param entityUuid the entity entering the portal
     * @param portal the portal being entered
     * @param targetServer the target server name
     * @return CompletableFuture that completes when the notification is sent
     */
    @NotNull
    public CompletableFuture<Void> sendPortalEntryNotification(
        @NotNull UUID entityUuid,
        @NotNull Portal portal,
        @NotNull String targetServer
    ) {
        if (!NetworkManager.isNetworkReady()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Network is not ready")
            );
        }

        PortalEntryNotification notification = new PortalEntryNotification(
            entityUuid,
            portal.id(),
            localServerName,
            targetServer
        );

        try {
            eventBus.sendToServer(NetworkChannels.PORTALS, targetServer, notification);
            logger.fine("Sent portal entry notification to " + targetServer +
                       " for entity " + entityUuid);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send portal entry notification to " + targetServer, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Broadcasts a portal update to all servers in the network.
     *
     * @param portal the portal to broadcast
     * @return CompletableFuture that completes when the broadcast is sent
     */
    @NotNull
    public CompletableFuture<Void> broadcastPortalUpdate(@NotNull Portal portal) {
        if (!NetworkManager.isNetworkReady()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Network is not ready")
            );
        }

        if (!portal.isCrossServer()) {
            return CompletableFuture.completedFuture(null);
        }

        PortalUpdateMessage update = new PortalUpdateMessage(
            portal.id(),
            portal,
            localServerName,
            System.currentTimeMillis()
        );

        try {
            eventBus.sendToAll(NetworkChannels.PORTALS, update);
            logger.fine("Broadcasted portal update for " + portal.id());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to broadcast portal update", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends a portal update to a specific server.
     *
     * @param portal the portal to send
     * @param targetServer the target server
     * @return CompletableFuture that completes when the update is sent
     */
    @NotNull
    public CompletableFuture<Void> sendPortalUpdate(@NotNull Portal portal, @NotNull String targetServer) {
        if (!NetworkManager.isNetworkReady()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Network is not ready")
            );
        }

        PortalUpdateMessage update = new PortalUpdateMessage(
            portal.id(),
            portal,
            localServerName,
            System.currentTimeMillis()
        );

        try {
            eventBus.sendToServer(NetworkChannels.PORTALS, targetServer, update);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send portal update to " + targetServer, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Requests portal sync from another server.
     *
     * @param world the world to sync
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param targetServer the server to request from
     * @return CompletableFuture that completes when the request is sent
     */
    @NotNull
    public CompletableFuture<Void> requestPortalSync(
        @NotNull String world,
        double x, double y, double z,
        @NotNull String targetServer
    ) {
        if (!NetworkManager.isNetworkReady()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Network is not ready")
            );
        }

        PortalSyncRequest request = new PortalSyncRequest(world, x, y, z, localServerName);

        try {
            eventBus.sendToServer(NetworkChannels.PORTALS, targetServer, request);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send portal sync request to " + targetServer, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Checks if this handler is registered.
     *
     * @return true if registered
     */
    public boolean isRegistered() {
        return subscription != null;
    }

    /**
     * Gets the local server name.
     *
     * @return the local server name
     */
    @NotNull
    public String getLocalServerName() {
        return localServerName;
    }

    // ==================== Message Records ====================

    /**
     * Notification sent when an entity enters a portal targeting another server.
     */
    public record PortalEntryNotification(
        @NotNull UUID entityUuid,
        @NotNull UUID portalId,
        @NotNull String sourceServer,
        @NotNull String targetServer
    ) {}

    /**
     * Message for broadcasting portal updates to the network.
     */
    public record PortalUpdateMessage(
        @NotNull UUID portalId,
        @Nullable Portal portal,
        @NotNull String sourceServer,
        long timestamp
    ) {}

    /**
     * Request for syncing portals from another server.
     */
    public record PortalSyncRequest(
        @NotNull String world,
        double x,
        double y,
        double z,
        @NotNull String requestingServer
    ) {}
}
