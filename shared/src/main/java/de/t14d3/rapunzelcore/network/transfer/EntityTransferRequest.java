package de.t14d3.rapunzelcore.network.transfer;

import de.t14d3.rapunzellib.nbt.SerializedEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing an entity transfer request between servers.
 * Contains all necessary information to serialize, transfer, and respawn an entity.
 */
public record EntityTransferRequest(
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
    Map<String, Object> metadata
) {
    
    /**
     * Creates a new builder for constructing EntityTransferRequest instances.
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for constructing EntityTransferRequest instances.
     * Provides a fluent API for setting all transfer parameters.
     */
    public static class Builder {
        private UUID entityUuid;
        private UUID ownerUuid;
        private String entityType;
        private String sourceServer;
        private String targetServer;
        private String targetWorld;
        private double targetX;
        private double targetY;
        private double targetZ;
        private float targetYaw;
        private float targetPitch;
        private SerializedEntity serializedEntity;
        private Map<String, Object> metadata = new HashMap<>();
        
        private Builder() {}
        
        public Builder entityUuid(UUID entityUuid) {
            this.entityUuid = entityUuid;
            return this;
        }
        
        public Builder ownerUuid(UUID ownerUuid) {
            this.ownerUuid = ownerUuid;
            return this;
        }
        
        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }
        
        public Builder sourceServer(String sourceServer) {
            this.sourceServer = sourceServer;
            return this;
        }
        
        public Builder targetServer(String targetServer) {
            this.targetServer = targetServer;
            return this;
        }
        
        public Builder targetWorld(String targetWorld) {
            this.targetWorld = targetWorld;
            return this;
        }
        
        public Builder targetX(double targetX) {
            this.targetX = targetX;
            return this;
        }
        
        public Builder targetY(double targetY) {
            this.targetY = targetY;
            return this;
        }
        
        public Builder targetZ(double targetZ) {
            this.targetZ = targetZ;
            return this;
        }
        
        public Builder targetYaw(float targetYaw) {
            this.targetYaw = targetYaw;
            return this;
        }
        
        public Builder targetPitch(float targetPitch) {
            this.targetPitch = targetPitch;
            return this;
        }
        
        public Builder serializedEntity(SerializedEntity serializedEntity) {
            this.serializedEntity = serializedEntity;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Builds the EntityTransferRequest instance.
         * @return a new immutable EntityTransferRequest
         * @throws NullPointerException if required fields are null
         */
        public EntityTransferRequest build() {
            Objects.requireNonNull(entityUuid, "entityUuid must not be null");
            Objects.requireNonNull(entityType, "entityType must not be null");
            Objects.requireNonNull(sourceServer, "sourceServer must not be null");
            Objects.requireNonNull(targetServer, "targetServer must not be null");
            Objects.requireNonNull(serializedEntity, "serializedEntity must not be null");
            
            return new EntityTransferRequest(
                entityUuid,
                ownerUuid,
                entityType,
                sourceServer,
                targetServer,
                targetWorld,
                targetX,
                targetY,
                targetZ,
                targetYaw,
                targetPitch,
                serializedEntity,
                new HashMap<>(metadata)
            );
        }
    }
    
    /**
     * Returns an immutable copy of the metadata map.
     * @return immutable metadata map
     */
    @Override
    public Map<String, Object> metadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Creates a copy of this request with updated target coordinates.
     * @param x new X coordinate
     * @param y new Y coordinate
     * @param z new Z coordinate
     * @return new EntityTransferRequest with updated position
     */
    public EntityTransferRequest withPosition(double x, double y, double z) {
        return new EntityTransferRequest(
            entityUuid, ownerUuid, entityType, sourceServer, targetServer,
            targetWorld, x, y, z, targetYaw, targetPitch, serializedEntity, metadata
        );
    }
    
    /**
     * Creates a copy of this request with updated target rotation.
     * @param yaw new yaw angle
     * @param pitch new pitch angle
     * @return new EntityTransferRequest with updated rotation
     */
    public EntityTransferRequest withRotation(float yaw, float pitch) {
        return new EntityTransferRequest(
            entityUuid, ownerUuid, entityType, sourceServer, targetServer,
            targetWorld, targetX, targetY, targetZ, yaw, pitch, serializedEntity, metadata
        );
    }
}
