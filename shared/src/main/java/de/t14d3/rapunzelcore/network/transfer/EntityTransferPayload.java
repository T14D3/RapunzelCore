package de.t14d3.rapunzelcore.network.transfer;

import de.t14d3.rapunzellib.nbt.SerializedEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Network payload for entity transfer messages.
 * Serializable record used for transmitting entity data across the network.
 * Contains factory methods for converting to/from EntityTransferRequest.
 */
public record EntityTransferPayload(
    UUID entityUuid,
    UUID ownerUuid,
    String entityType,
    String sourceServer,
    String targetServer,
    String targetWorld,
    double targetX,
    double targetY,
    double targetZ,
    float targetYaw,
    float targetPitch,
    SerializedEntity serializedEntity,
    Map<String, String> metadata
) {
    
    /**
     * Compact constructor to defensively copy metadata.
     */
    public EntityTransferPayload {
        metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    /**
     * Creates an EntityTransferPayload from an EntityTransferRequest.
     * Converts the metadata map to String values for network transmission.
     * 
     * @param request the transfer request to convert
     * @return a new payload ready for network transmission
     * @throws NullPointerException if request is null
     */
    public static EntityTransferPayload fromRequest(EntityTransferRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        
        // Convert metadata to String values for network safety
        Map<String, String> stringMetadata = new HashMap<>();
        if (request.metadata() != null) {
            for (Map.Entry<String, Object> entry : request.metadata().entrySet()) {
                if (entry.getValue() != null) {
                    stringMetadata.put(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        
        return new EntityTransferPayload(
            request.entityUuid(),
            request.ownerUuid(),
            request.entityType(),
            request.sourceServer(),
            request.targetServer(),
            request.targetWorld(),
            request.targetX(),
            request.targetY(),
            request.targetZ(),
            request.targetYaw(),
            request.targetPitch(),
            request.serializedEntity(),
            stringMetadata
        );
    }
    
    /**
     * Converts this payload back to an EntityTransferRequest.
     * Reconstructs the metadata map from string values.
     * 
     * @return a new EntityTransferRequest with all payload data
     */
    public EntityTransferRequest toRequest() {
        // Convert string metadata back to Object map
        Map<String, Object> objectMetadata = new HashMap<>(metadata);
        
        return EntityTransferRequest.builder()
            .entityUuid(entityUuid)
            .ownerUuid(ownerUuid)
            .entityType(entityType)
            .sourceServer(sourceServer)
            .targetServer(targetServer)
            .targetWorld(targetWorld)
            .targetX(targetX)
            .targetY(targetY)
            .targetZ(targetZ)
            .targetYaw(targetYaw)
            .targetPitch(targetPitch)
            .serializedEntity(serializedEntity)
            .metadata(objectMetadata)
            .build();
    }
    
    /**
     * Creates a builder pre-populated with this payload's data.
     * Useful for creating modified copies.
     * 
     * @return a builder with current values
     */
    public EntityTransferRequest.Builder toRequestBuilder() {
        return EntityTransferRequest.builder()
            .entityUuid(entityUuid)
            .ownerUuid(ownerUuid)
            .entityType(entityType)
            .sourceServer(sourceServer)
            .targetServer(targetServer)
            .targetWorld(targetWorld)
            .targetX(targetX)
            .targetY(targetY)
            .targetZ(targetZ)
            .targetYaw(targetYaw)
            .targetPitch(targetPitch)
            .serializedEntity(serializedEntity)
            .metadata(new HashMap<>(metadata));
    }
    
    /**
     * Returns an immutable copy of the metadata map.
     * @return immutable metadata map
     */
    @Override
    public Map<String, String> metadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Checks if this payload is targeted at the given server.
     * @param serverName the server name to check
     * @return true if this payload is for the specified server
     */
    public boolean isTargetedAt(String serverName) {
        return targetServer != null && targetServer.equals(serverName);
    }
    
    /**
     * Checks if this payload originated from the given server.
     * @param serverName the server name to check
     * @return true if this payload originated from the specified server
     */
    public boolean originatedFrom(String serverName) {
        return sourceServer != null && sourceServer.equals(serverName);
    }
    
    /**
     * Creates a copy with a new target server.
     * Useful for forwarding/redirecting transfers.
     * @param newTargetServer the new target server
     * @return a new payload with updated target
     */
    public EntityTransferPayload withTargetServer(String newTargetServer) {
        return new EntityTransferPayload(
            entityUuid, ownerUuid, entityType, sourceServer,
            newTargetServer, targetWorld, targetX, targetY, targetZ,
            targetYaw, targetPitch, serializedEntity, metadata
        );
    }
}
