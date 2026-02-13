package de.t14d3.rapunzelcore.velocity.handlers;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferPayload;
import de.t14d3.rapunzelcore.velocity.VelocityNetworkBridge;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Handler for entity transfer messages on the Velocity proxy.
 *
 * This handler processes messages on the ENTITY_TRANSFER channel,
 * deserializes EntityTransferPayload objects, and delegates all forwarding
 * logic to VelocityNetworkBridge for centralized management.
 *
 * Key responsibilities:
 * - Deserialize incoming entity transfer payloads
 * - Delegate routing to VelocityNetworkBridge
 * - Handle transfer confirmations
 */
public class EntityTransferHandler implements MessageListener {

    private final ProxyServer proxy;
    private final Logger logger;
    private final VelocityNetworkBridge networkBridge;
    private final Gson gson;

    /**
     * Creates a new EntityTransferHandler instance.
     *
     * @param proxy the Velocity proxy server
     * @param logger the logger instance
     * @param networkBridge the network bridge for forwarding messages
     */
    public EntityTransferHandler(
            @NotNull ProxyServer proxy,
            @NotNull Logger logger,
            @NotNull VelocityNetworkBridge networkBridge) {
        this.proxy = proxy;
        this.logger = logger;
        this.networkBridge = networkBridge;
        this.gson = JsonCodecs.gson();
    }

    /**
     * Handles incoming entity transfer messages.
     *
     * This method is called when a message is received on the ENTITY_TRANSFER channel.
     * It deserializes the payload and delegates routing to VelocityNetworkBridge.
     *
     * @param channel the channel the message was received on
     * @param data the message data as string
     * @param sourceServer the source server name
     */
    @Override
    public void onMessage(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
        if (!NetworkChannels.ENTITY_TRANSFER.equals(channel)) {
            return;
        }

        try {
            // Try to deserialize as EntityTransferPayload
            EntityTransferPayload payload = gson.fromJson(data, EntityTransferPayload.class);

            if (payload == null) {
                logger.warn("Received null entity transfer payload");
                return;
            }

            logger.debug("Received entity transfer for {} from '{}' to '{}'",
                    payload.entityUuid(), payload.sourceServer(), payload.targetServer());

            // Delegate routing to VelocityNetworkBridge
            routeEntityTransfer(payload, sourceServer);

        } catch (Exception e) {
            logger.error("Failed to parse entity transfer message: {}", e.getMessage(), e);
        }
    }

    /**
     * Routes an entity transfer to the appropriate target server.
     *
     * Delegates all forwarding logic to VelocityNetworkBridge for centralized
     * management of entity transfers.
     *
     * @param payload the entity transfer payload
     * @param sourceServer the source server name
     */
    private void routeEntityTransfer(@NotNull EntityTransferPayload payload, @NotNull String sourceServer) {
        String targetServer = payload.targetServer();

        // Check if this is a transfer confirmation (no serialized entity data)
        if (payload.serializedEntity() == null || payload.serializedEntity().nbtData().length == 0) {
            handleTransferConfirmation(payload, sourceServer);
            return;
        }

        // Delegate forwarding to VelocityNetworkBridge
        boolean forwarded = networkBridge.forwardEntityTransfer(payload, gson);

        if (forwarded) {
            logger.debug("Entity transfer for {} forwarded to server '{}'",
                    payload.entityUuid(), targetServer);
        } else {
            logger.warn("Failed to forward entity transfer for {} to server '{}'",
                    payload.entityUuid(), targetServer);

            // Send failure confirmation back to source
            networkBridge.sendTransferConfirmation(
                    payload.entityUuid(),
                    null,
                    sourceServer,
                    false,
                    "Failed to forward transfer to target server"
            );
        }
    }

    /**
     * Handles a transfer confirmation message.
     *
     * When a transfer is confirmed (either success or failure), this method
     * updates the pending transfer tracking and notifies the source server.
     *
     * @param payload the entity transfer payload (contains confirmation data)
     * @param sourceServer the source server name
     */
    private void handleTransferConfirmation(@NotNull EntityTransferPayload payload, @NotNull String sourceServer) {
        UUID entityUuid = payload.entityUuid();

        // Check if there's a pending transfer for this entity
        if (networkBridge.hasPendingTransfer(entityUuid)) {
            // Complete the pending transfer
            networkBridge.completeTransfer(entityUuid, true);

            logger.debug("Confirmed entity transfer for {} from '{}'", entityUuid, sourceServer);
        } else {
            logger.debug("Received confirmation for unknown entity transfer {}", entityUuid);
        }

        // Forward confirmation to source server if different from current
        if (!"unknown".equals(sourceServer)) {
            networkBridge.sendTransferConfirmation(
                    entityUuid,
                    entityUuid, // Same entity ID for confirmation
                    sourceServer,
                    true,
                    "Transfer confirmed"
            );
        }
    }

    /**
     * Gets the VelocityNetworkBridge instance.
     *
     * @return the network bridge
     */
    public VelocityNetworkBridge getNetworkBridge() {
        return networkBridge;
    }

    /**
     * Gets the ProxyServer instance.
     *
     * @return the proxy server
     */
    public ProxyServer getProxy() {
        return proxy;
    }

    /**
     * Gets the logger instance.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }
}
