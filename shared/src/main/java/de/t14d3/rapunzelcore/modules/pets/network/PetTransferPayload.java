package de.t14d3.rapunzelcore.modules.pets.network;

import de.t14d3.rapunzelcore.network.transfer.EntityTransferPayload;
import de.t14d3.rapunzellib.nbt.SerializedEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Network payload for pet transfer messages.
 * Serializable record used for transmitting pet data across the network.
 * Provides conversion methods to/from the shared {@link EntityTransferPayload}.
 */
public record PetTransferPayload(
    UUID petId,
    UUID ownerUuid,
    String targetServer,
    String targetWorld,
    double targetX,
    double targetY,
    double targetZ,
    SerializedEntity serializedEntity,
    String petType,
    String customName,
    String sourceServer
) {
    public static final String CHANNEL = "rapunzelcore:pet_transfer";

    /**
     * Creates a payload from a transfer request.
     *
     * @param request the transfer request
     * @param sourceServer the source server name
     * @return the payload
     */
    public static PetTransferPayload fromRequest(PetTransferRequest request, String sourceServer) {
        Objects.requireNonNull(request, "request must not be null");
        return new PetTransferPayload(
            request.petId(),
            request.ownerUuid(),
            request.targetServer(),
            request.targetWorld(),
            request.targetX(),
            request.targetY(),
            request.targetZ(),
            request.serializedEntity(),
            request.petType(),
            request.customName(),
            sourceServer
        );
    }

    /**
     * Converts this payload back to a transfer request.
     *
     * @return the transfer request
     */
    public PetTransferRequest toRequest() {
        return PetTransferRequest.builder()
            .petId(petId)
            .ownerUuid(ownerUuid)
            .targetServer(targetServer)
            .targetWorld(targetWorld)
            .targetLocation(targetX, targetY, targetZ)
            .serializedEntity(serializedEntity)
            .petType(petType)
            .customName(customName)
            .build();
    }

    /**
     * Converts this pet transfer payload to a generic entity transfer payload.
     * Maps pet-specific fields to entity transfer format for use with the shared service.
     *
     * @return a new EntityTransferPayload containing this pet's data
     */
    public EntityTransferPayload toEntityTransferPayload() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("petType", petType);
        metadata.put("transferType", "PET");
        if (customName != null) {
            metadata.put("customName", customName);
        }

        return new EntityTransferPayload(
            petId,
            ownerUuid,
            petType,
            sourceServer,
            targetServer,
            targetWorld,
            targetX,
            targetY,
            targetZ,
            0.0f, // yaw
            0.0f, // pitch
            serializedEntity,
            metadata
        );
    }

    /**
     * Creates a PetTransferPayload from a generic entity transfer payload.
     * Extracts pet-specific fields from the entity payload metadata.
     *
     * @param entityPayload the entity transfer payload to convert
     * @return a new PetTransferPayload with data extracted from the entity payload
     * @throws NullPointerException if entityPayload is null
     */
    public static PetTransferPayload fromEntityTransferPayload(EntityTransferPayload entityPayload) {
        Objects.requireNonNull(entityPayload, "entityPayload must not be null");

        Map<String, String> metadata = entityPayload.metadata();
        String petType = entityPayload.entityType();
        String customName = metadata != null ? metadata.get("customName") : null;
        String source = metadata != null ? metadata.get("sourceServer") : null;

        return new PetTransferPayload(
            entityPayload.entityUuid(),
            entityPayload.ownerUuid(),
            entityPayload.targetServer(),
            entityPayload.targetWorld(),
            entityPayload.targetX(),
            entityPayload.targetY(),
            entityPayload.targetZ(),
            entityPayload.serializedEntity(),
            petType,
            customName,
            entityPayload.sourceServer()
        );
    }

    /**
     * Checks if this payload is targeted at the given server.
     *
     * @param serverName the server name to check
     * @return true if this payload is for the specified server
     */
    public boolean isTargetedAt(String serverName) {
        return targetServer != null && targetServer.equals(serverName);
    }

    /**
     * Checks if this payload originated from the given server.
     *
     * @param serverName the server name to check
     * @return true if this payload originated from the specified server
     */
    public boolean originatedFrom(String serverName) {
        return sourceServer != null && sourceServer.equals(serverName);
    }
}
