package de.t14d3.rapunzelcore.modules.pets.network;

import de.t14d3.rapunzelcore.network.transfer.EntityTransferRequest;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

/**
 * Request to transfer a pet to another server.
 * Wraps pet-specific transfer data and provides conversion to/from
 * the shared {@link EntityTransferRequest} for cross-server communication.
 *
 * @param petId the unique pet ID
 * @param ownerUuid the pet owner's UUID
 * @param targetServer the target server name
 * @param targetWorld the target world name (optional)
 * @param targetX target X coordinate
 * @param targetY target Y coordinate
 * @param targetZ target Z coordinate
 * @param serializedEntity the serialized entity data
 * @param petType the type of pet (e.g., "wolf", "cat", "parrot")
 * @param customName the custom name of the pet, if any
 */
public record PetTransferRequest(
    @NotNull UUID petId,
    @NotNull UUID ownerUuid,
    @NotNull String targetServer,
    @Nullable String targetWorld,
    double targetX,
    double targetY,
    double targetZ,
    @NotNull SerializedEntity serializedEntity,
    @NotNull String petType,
    @Nullable String customName
) {

    /**
     * Creates a new builder for constructing PetTransferRequest instances.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts this pet transfer request to a generic entity transfer request.
     * Maps pet-specific fields to entity transfer format for use with the shared service.
     *
     * @return a new EntityTransferRequest containing this pet's data
     */
    public EntityTransferRequest toEntityTransferRequest() {
        return EntityTransferRequest.builder()
            .entityUuid(petId)
            .ownerUuid(ownerUuid)
            .entityType(petType)
            .sourceServer("") // To be set by transfer service
            .targetServer(targetServer)
            .targetWorld(targetWorld)
            .targetX(targetX)
            .targetY(targetY)
            .targetZ(targetZ)
            .targetYaw(0.0f)
            .targetPitch(0.0f)
            .serializedEntity(serializedEntity)
            .addMetadata("petType", petType)
            .addMetadata("customName", customName)
            .addMetadata("transferType", "PET")
            .build();
    }

    /**
     * Creates a PetTransferRequest from a generic entity transfer request.
     * Extracts pet-specific fields from the entity request metadata.
     *
     * @param entityRequest the entity transfer request to convert
     * @return a new PetTransferRequest with data extracted from the entity request
     * @throws NullPointerException if entityRequest is null or required fields are missing
     */
    public static PetTransferRequest fromEntityTransferRequest(@NotNull EntityTransferRequest entityRequest) {
        Objects.requireNonNull(entityRequest, "entityRequest must not be null");

        String petType = entityRequest.entityType();
        String customName = null;

        if (entityRequest.metadata() != null) {
            Object nameObj = entityRequest.metadata().get("customName");
            if (nameObj != null) {
                customName = nameObj.toString();
            }
        }

        return new PetTransferRequest(
            entityRequest.entityUuid(),
            entityRequest.ownerUuid(),
            entityRequest.targetServer(),
            entityRequest.targetWorld(),
            entityRequest.targetX(),
            entityRequest.targetY(),
            entityRequest.targetZ(),
            entityRequest.serializedEntity(),
            petType,
            customName
        );
    }

    /**
     * Builder class for constructing PetTransferRequest instances.
     * Provides a fluent API for setting all transfer parameters.
     */
    public static class Builder {
        private UUID petId;
        private UUID ownerUuid;
        private String targetServer;
        private String targetWorld;
        private double targetX;
        private double targetY;
        private double targetZ;
        private SerializedEntity serializedEntity;
        private String petType;
        private String customName;

        private Builder() {}

        public Builder petId(UUID petId) {
            this.petId = petId;
            return this;
        }

        public Builder ownerUuid(UUID ownerUuid) {
            this.ownerUuid = ownerUuid;
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

        public Builder targetLocation(double x, double y, double z) {
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
            return this;
        }

        public Builder serializedEntity(SerializedEntity serializedEntity) {
            this.serializedEntity = serializedEntity;
            return this;
        }

        public Builder petType(String petType) {
            this.petType = petType;
            return this;
        }

        public Builder customName(String customName) {
            this.customName = customName;
            return this;
        }

        /**
         * Builds the PetTransferRequest instance.
         *
         * @return a new immutable PetTransferRequest
         * @throws NullPointerException if required fields are null
         */
        public PetTransferRequest build() {
            Objects.requireNonNull(petId, "petId must not be null");
            Objects.requireNonNull(ownerUuid, "ownerUuid must not be null");
            Objects.requireNonNull(targetServer, "targetServer must not be null");
            Objects.requireNonNull(serializedEntity, "serializedEntity must not be null");
            Objects.requireNonNull(petType, "petType must not be null");

            return new PetTransferRequest(
                petId,
                ownerUuid,
                targetServer,
                targetWorld,
                targetX,
                targetY,
                targetZ,
                serializedEntity,
                petType,
                customName
            );
        }
    }
}
