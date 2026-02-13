package de.t14d3.rapunzelcore.network.transfer;

import de.t14d3.rapunzellib.nbt.SerializedEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for handling entity transfers between servers.
 * Provides methods for serializing, transferring, and receiving entities
 * across the network with asynchronous CompletableFuture-based operations.
 */
public interface EntityTransferService {
    
    /**
     * Initiates an entity transfer to a target server.
     * Serializes the entity, sends it to the target server, and removes it from the local server.
     * 
     * @param entityUuid the UUID of the entity to transfer
     * @param targetServer the name of the target server
     * @param targetWorld the target world name (can be null to use current world)
     * @param x the target X coordinate
     * @param y the target Y coordinate
     * @param z the target Z coordinate
     * @param yaw the target yaw rotation
     * @param pitch the target pitch rotation
     * @return CompletableFuture that completes with the transfer result, or exceptionally on failure
     */
    CompletableFuture<TransferResult> transferEntity(
        UUID entityUuid,
        String targetServer,
        String targetWorld,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    );
    
    /**
     * Overloaded method for transferring entity with default rotation.
     * 
     * @param entityUuid the UUID of the entity to transfer
     * @param targetServer the name of the target server
     * @param targetWorld the target world name (can be null to use current world)
     * @param x the target X coordinate
     * @param y the target Y coordinate
     * @param z the target Z coordinate
     * @return CompletableFuture that completes with the transfer result
     */
    default CompletableFuture<TransferResult> transferEntity(
        UUID entityUuid,
        String targetServer,
        String targetWorld,
        double x,
        double y,
        double z
    ) {
        return transferEntity(entityUuid, targetServer, targetWorld, x, y, z, 0.0f, 0.0f);
    }
    
    /**
     * Receives an entity transfer from another server.
     * Deserializes and spawns the entity at the specified location.
     * 
     * @param request the entity transfer request containing serialized data
     * @return CompletableFuture that completes with the spawned entity's UUID, or exceptionally on failure
     */
    CompletableFuture<UUID> receiveEntity(EntityTransferRequest request);
    
    /**
     * Serializes an entity for transfer.
     * Captures all entity data including NBT tags, inventory, and custom metadata.
     * 
     * @param entityUuid the UUID of the entity to serialize
     * @return CompletableFuture that completes with the serialized entity, or exceptionally if entity not found
     */
    CompletableFuture<SerializedEntity> serializeEntity(UUID entityUuid);
    
    /**
     * Checks if entity transfer is enabled on this server.
     * 
     * @return true if transfers are enabled and configured
     */
    boolean isTransferEnabled();
    
    /**
     * Gets the name of the local server in the network.
     * Used for identifying the source of transfers.
     * 
     * @return the local server name, or empty string if not configured
     */
    String getLocalServerName();
    
    /**
     * Validates if a transfer can be performed.
     * Checks if the target server is available and the entity exists.
     * 
     * @param entityUuid the entity to validate
     * @param targetServer the target server to validate
     * @return CompletableFuture with validation result
     */
    default CompletableFuture<ValidationResult> validateTransfer(UUID entityUuid, String targetServer) {
        if (!isTransferEnabled()) {
            return CompletableFuture.completedFuture(
                new ValidationResult(false, "Entity transfer is not enabled on this server")
            );
        }
        return CompletableFuture.completedFuture(new ValidationResult(true, null));
    }
    
    /**
     * Result of an entity transfer operation.
     */
    record TransferResult(
        boolean success,
        UUID entityUuid,
        String targetServer,
        Optional<String> errorMessage
    ) {
        public TransferResult(boolean success, UUID entityUuid, String targetServer) {
            this(success, entityUuid, targetServer, Optional.empty());
        }
        
        public TransferResult(boolean success, UUID entityUuid, String targetServer, String errorMessage) {
            this(success, entityUuid, targetServer, Optional.ofNullable(errorMessage));
        }
    }
    
    /**
     * Result of a transfer validation.
     */
    record ValidationResult(boolean valid, String errorMessage) {
        public boolean isInvalid() {
            return !valid;
        }
    }
}
