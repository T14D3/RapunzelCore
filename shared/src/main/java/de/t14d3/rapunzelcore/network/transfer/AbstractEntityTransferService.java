package de.t14d3.rapunzelcore.network.transfer;

import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.network.NetworkManager;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base implementation of EntityTransferService.
 * Handles network communication and provides common functionality for
 * platform-specific implementations.
 */
public abstract class AbstractEntityTransferService implements EntityTransferService {
    
    protected final Logger logger;
    protected final NetworkEventBus eventBus;
    protected final String localServerName;
    protected final boolean transferEnabled;
    protected final Executor executor;
    
    // Track pending transfers
    protected final Map<UUID, CompletableFuture<TransferResult>> pendingTransfers = new ConcurrentHashMap<>();
    
    private NetworkEventBus.Subscription subscription;
    
    /**
     * Creates a new AbstractEntityTransferService.
     *
     * @param logger the logger instance
     * @param messenger the messenger for network communication
     * @param localServerName the name of this server in the network
     * @param transferEnabled whether entity transfer is enabled
     */
    protected AbstractEntityTransferService(
        Logger logger,
        Messenger messenger,
        String localServerName,
        boolean transferEnabled
    ) {
        this.logger = logger;
        this.eventBus = new NetworkEventBus(messenger);
        this.localServerName = localServerName;
        this.transferEnabled = transferEnabled;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EntityTransfer-Worker");
            t.setDaemon(true);
            return t;
        });
        
        if (transferEnabled) {
            registerNetworkHandlers();
        }
    }
    
    /**
     * Registers network handlers for entity transfer messages.
     */
    protected void registerNetworkHandlers() {
        if (eventBus == null) {
            logger.warning("NetworkEventBus is null, entity transfer will not function");
            return;
        }
        
        // Register incoming transfer handler
        subscription = eventBus.register(NetworkChannels.ENTITY_TRANSFER, Object.class, (payload, sender) -> {
            try {
                if (payload instanceof EntityTransferPayload entityPayload) {
                    handleIncomingTransfer(entityPayload);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error handling incoming entity transfer", e);
            }
        });
        
        logger.info("Entity transfer network handlers registered");
    }
    
    /**
     * Handles an incoming entity transfer payload from another server.
     *
     * @param payload the transfer payload
     */
    protected void handleIncomingTransfer(EntityTransferPayload payload) {
        if (!isTransferEnabled()) {
            logger.warning("Received entity transfer but transfers are disabled on this server");
            return;
        }
        
        // Validate this is for us
        if (!payload.isTargetedAt(localServerName)) {
            logger.fine("Ignoring entity transfer not targeted at this server: " + payload.targetServer());
            return;
        }
        
        logger.info("Receiving entity transfer: " + payload.entityUuid() +
                   " from " + payload.sourceServer() +
                   " type: " + payload.entityType());
        
        // Convert to request and process
        EntityTransferRequest request = payload.toRequest();
        
        receiveEntity(request).whenComplete((spawnedUuid, error) -> {
            if (error != null) {
                logger.log(Level.SEVERE, "Failed to spawn transferred entity: " + payload.entityUuid(), error);
                sendTransferResponse(payload, false, error.getMessage());
            } else {
                logger.info("Successfully spawned transferred entity: " + spawnedUuid);
                sendTransferResponse(payload, true, null);
            }
        });
    }
    
    /**
     * Sends a transfer response back to the source server.
     *
     * @param originalPayload the original transfer payload
     * @param success whether the transfer succeeded
     * @param errorMessage error message if failed
     */
    protected void sendTransferResponse(EntityTransferPayload originalPayload, boolean success, String errorMessage) {
        if (eventBus == null) return;
        
        // Build response payload
        ResponsePayload response = new ResponsePayload(
            originalPayload.entityUuid(),
            success,
            errorMessage,
            localServerName
        );
        
        eventBus.sendToServer(NetworkChannels.ENTITY_TRANSFER + "_response", 
                             originalPayload.sourceServer(), 
                             response);
    }
    
    @Override
    public CompletableFuture<TransferResult> transferEntity(
        UUID entityUuid,
        String targetServer,
        String targetWorld,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        if (!isTransferEnabled()) {
            return CompletableFuture.completedFuture(
                new TransferResult(false, entityUuid, targetServer, "Entity transfer is disabled")
            );
        }
        
        if (eventBus == null) {
            return CompletableFuture.completedFuture(
                new TransferResult(false, entityUuid, targetServer, "NetworkEventBus not available")
            );
        }
        
        logger.info("Initiating entity transfer: " + entityUuid + " to " + targetServer);
        
        CompletableFuture<TransferResult> future = new CompletableFuture<>();
        pendingTransfers.put(entityUuid, future);
        
        // First serialize the entity
        serializeEntity(entityUuid).whenCompleteAsync((serialized, error) -> {
            if (error != null) {
                logger.log(Level.SEVERE, "Failed to serialize entity: " + entityUuid, error);
                pendingTransfers.remove(entityUuid);
                future.complete(new TransferResult(false, entityUuid, targetServer,
                    "Serialization failed: " + error.getMessage()));
                return;
            }
            
            // Build and send transfer request
            EntityTransferRequest request = EntityTransferRequest.builder()
                .entityUuid(entityUuid)
                .ownerUuid(getEntityOwner(entityUuid))
                .entityType(getEntityType(entityUuid))
                .sourceServer(localServerName)
                .targetServer(targetServer)
                .targetWorld(targetWorld)
                .targetX(x)
                .targetY(y)
                .targetZ(z)
                .targetYaw(yaw)
                .targetPitch(pitch)
                .serializedEntity(serialized)
                .metadata(getEntityMetadata(entityUuid))
                .build();
            
            EntityTransferPayload payload = EntityTransferPayload.fromRequest(request);
            
            // Send to target server
            eventBus.sendToServer(NetworkChannels.ENTITY_TRANSFER, targetServer, payload);
            
            // Remove entity from local server after successful serialization
            removeLocalEntity(entityUuid);
            
            // Complete the future (in real implementation, wait for acknowledgment)
            future.complete(new TransferResult(true, entityUuid, targetServer));
            pendingTransfers.remove(entityUuid);
            
        }, executor);
        
        return future;
    }
    
    @Override
    public boolean isTransferEnabled() {
        return transferEnabled;
    }
    
    @Override
    public String getLocalServerName() {
        return localServerName;
    }
    
    /**
     * Gets the owner UUID of an entity, if applicable.
     * Override in platform-specific implementation.
     *
     * @param entityUuid the entity UUID
     * @return the owner UUID, or null if no owner
     */
    protected abstract UUID getEntityOwner(UUID entityUuid);
    
    /**
     * Gets the entity type identifier.
     * Override in platform-specific implementation.
     *
     * @param entityUuid the entity UUID
     * @return the entity type string
     */
    protected abstract String getEntityType(UUID entityUuid);
    
    /**
     * Gets additional metadata for an entity.
     * Override in platform-specific implementation.
     *
     * @param entityUuid the entity UUID
     * @return metadata map
     */
    protected abstract Map<String, Object> getEntityMetadata(UUID entityUuid);
    
    /**
     * Removes an entity from the local server after successful transfer.
     * Override in platform-specific implementation.
     *
     * @param entityUuid the entity UUID to remove
     */
    protected abstract void removeLocalEntity(UUID entityUuid);
    
    /**
     * Response payload for transfer acknowledgments.
     */
    protected record ResponsePayload(
        UUID entityUuid,
        boolean success,
        String errorMessage,
        String respondingServer
    ) {}
}
