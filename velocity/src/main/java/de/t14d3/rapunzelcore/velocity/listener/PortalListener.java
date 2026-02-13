package de.t14d3.rapunzelcore.velocity.listener;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzelcore.modules.portals.network.PortalNetworkMessage;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.velocity.VelocityNetworkBridge;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;


/**
 * Listener for portal-related network messages on the Velocity proxy.
 *
 * <p>This listener handles cross-server portal synchronization by processing
 * messages on the PORTALS channel and forwarding them between backend servers.</p>
 *
 * <p>Supported message types:</p>
 * <ul>
 *   <li>PortalSync - Broadcasts portal creation/updates to all servers</li>
 *   <li>PortalDelete - Notifies servers of portal deletions</li>
 *   <li>EntityPortalEntry - Routes entities entering portals to target servers</li>
 *   <li>TransferConfirm - Confirms entity transfer completion</li>
 *   <li>PortalDataRequest - Requests portal data from other servers</li>
 *   <li>PortalDataResponse - Responds with portal data</li>
 * </ul>
 */
public class PortalListener implements MessageListener {

    private final ProxyServer proxy;
    private final Logger logger;
    private final VelocityNetworkBridge networkBridge;
    private final Gson gson;
    
    // Track which server sent which message for routing
    private static final String MESSAGE_TYPE_KEY = "type";
    private static final String SOURCE_SERVER_KEY = "sourceServer";

    /**
     * Creates a new PortalListener instance.
     *
     * @param proxy the Velocity proxy server
     * @param logger the logger instance
     * @param networkBridge the network bridge for forwarding messages
     */
    public PortalListener(
            @NotNull ProxyServer proxy,
            @NotNull Logger logger,
            @NotNull VelocityNetworkBridge networkBridge) {
        this.proxy = proxy;
        this.logger = logger;
        this.networkBridge = networkBridge;
        this.gson = JsonCodecs.gson();
    }

    /**
     * Handles incoming portal network messages.
     *
     * <p>This method is called when a message is received on the PORTALS channel.
     * It parses the message type and routes it appropriately.</p>
     *
     * @param channel the channel the message was received on
     * @param data the message data as byte array
     * @param sourceServer the source server name
     */
    @Override
    public void onMessage(@NotNull String channel, @NotNull String data, @NotNull String sourceServer) {
        if (!NetworkChannels.PORTALS.equals(channel)) {
            return;
        }
        
        try {
            JsonObject jsonObject = JsonParser.parseString(data).getAsJsonObject();
            String messageType = jsonObject.has(MESSAGE_TYPE_KEY)
                ? jsonObject.get(MESSAGE_TYPE_KEY).getAsString()
                : null;
            
            if (messageType == null) {
                logger.warn("Received portal message without type field");
                return;
            }
            
            // Route based on message type
            switch (messageType) {
                case "PortalSync" -> handlePortalSync(data, sourceServer);
                case "PortalDelete" -> handlePortalDelete(data, sourceServer);
                case "EntityPortalEntry" -> handleEntityPortalEntry(data, sourceServer);
                case "TransferConfirm" -> handleTransferConfirm(data, sourceServer);
                case "PortalDataRequest" -> handlePortalDataRequest(data, sourceServer);
                case "PortalDataResponse" -> handlePortalDataResponse(data, sourceServer);
                default -> logger.warn("Unknown portal message type: {}", messageType);
            }
            
        } catch (Exception e) {
            logger.error("Failed to parse portal network message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PortalSync messages by broadcasting to all other servers.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handlePortalSync(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.PortalSync message = gson.fromJson(json, PortalNetworkMessage.PortalSync.class);
            
            logger.debug("Received PortalSync from '{}' for portal {}",
                sourceServer, message.portal().id());
            
            // Broadcast to all servers except the source
            networkBridge.broadcastToAll(NetworkChannels.PORTALS, json, sourceServer);
            
        } catch (Exception e) {
            logger.error("Failed to handle PortalSync: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PortalDelete messages by broadcasting to all other servers.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handlePortalDelete(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.PortalDelete message = gson.fromJson(json, PortalNetworkMessage.PortalDelete.class);
            
            logger.debug("Received PortalDelete from '{}' for portal {}",
                sourceServer, message.portalId());
            
            // Broadcast to all servers except the source
            networkBridge.broadcastToAll(NetworkChannels.PORTALS, json, sourceServer);
            
        } catch (Exception e) {
            logger.error("Failed to handle PortalDelete: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles EntityPortalEntry messages by routing entities to target servers.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handleEntityPortalEntry(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.EntityPortalEntry message = gson.fromJson(json, PortalNetworkMessage.EntityPortalEntry.class);
            
            logger.debug("Received EntityPortalEntry from '{}' for entity {} to server {}",
                sourceServer, message.entityUuid(), message.targetServer());
            
            // Forward to the target server only
            networkBridge.forwardToServer(NetworkChannels.PORTALS, json, message.targetServer());
            
        } catch (Exception e) {
            logger.error("Failed to handle EntityPortalEntry: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles TransferConfirm messages by broadcasting to all servers.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handleTransferConfirm(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.TransferConfirm message = gson.fromJson(json, PortalNetworkMessage.TransferConfirm.class);
            
            logger.debug("Received TransferConfirm from '{}' for entity {} (success: {})",
                sourceServer, message.originalEntityUuid(), message.success());
            
            // Broadcast to all servers
            networkBridge.broadcastToAll(NetworkChannels.PORTALS, json, null);
            
        } catch (Exception e) {
            logger.error("Failed to handle TransferConfirm: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PortalDataRequest messages by forwarding to all servers.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handlePortalDataRequest(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.PortalDataRequest message = gson.fromJson(json, PortalNetworkMessage.PortalDataRequest.class);
            
            logger.debug("Received PortalDataRequest from '{}'", sourceServer);
            
            // Broadcast to all servers except the source
            networkBridge.broadcastToAll(NetworkChannels.PORTALS, json, sourceServer);
            
        } catch (Exception e) {
            logger.error("Failed to handle PortalDataRequest: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles PortalDataResponse messages by forwarding to the requesting server.
     *
     * @param json the JSON message
     * @param sourceServer the source server name
     */
    private void handlePortalDataResponse(@NotNull String json, @NotNull String sourceServer) {
        try {
            PortalNetworkMessage.PortalDataResponse message = gson.fromJson(json, PortalNetworkMessage.PortalDataResponse.class);
            
            logger.debug("Received PortalDataResponse from '{}' with {} portals",
                sourceServer, message.portals().size());
            
            // Broadcast to all servers (the requesting server will filter as needed)
            networkBridge.broadcastToAll(NetworkChannels.PORTALS, json, null);
            
        } catch (Exception e) {
            logger.error("Failed to handle PortalDataResponse: {}", e.getMessage(), e);
        }
    }
}
