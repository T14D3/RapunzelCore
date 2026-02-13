package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.modules.pets.network.PetTransferRequest;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferRequest;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling cross-server pet transfers.
 * Extends the shared {@link EntityTransferService} to provide pet-specific
 * transfer functionality while leveraging the common entity transfer infrastructure.
 *
 * <p>This interface defines the contract for pet transfer operations, including:</p>
 * <ul>
 *   <li>Transferring pets to other servers</li>
 *   <li>Receiving pets from other servers</li>
 *   <li>Serializing pet entities for transfer</li>
 * </ul>
 *
 * <p>Implementations should delegate to the shared entity transfer service
 * for the actual network operations while handling pet-specific logic locally.</p>
 *
 * @see EntityTransferService
 * @see PetTransferAdapter
 * @see PetTransferRequest
 */
public interface PetTransferService extends EntityTransferService {

    /**
     * Transfers a pet to another server.
     *
     * @param petId the pet ID
     * @param ownerUuid the owner UUID
     * @param targetServer the target server name
     * @param targetWorld the target world name (optional)
     * @param targetX target X coordinate
     * @param targetY target Y coordinate
     * @param targetZ target Z coordinate
     * @return a future that completes when the transfer is initiated
     */
    CompletableFuture<Boolean> transferPet(
        @NotNull UUID petId,
        @NotNull UUID ownerUuid,
        @NotNull String targetServer,
        @Nullable String targetWorld,
        double targetX,
        double targetY,
        double targetZ
    );

    /**
     * Transfers a pet using a pre-built request.
     *
     * @param request the transfer request
     * @return a future that completes when the transfer is initiated
     */
    CompletableFuture<Boolean> transferPet(@NotNull PetTransferRequest request);

    /**
     * Handles an incoming pet transfer from another server.
     *
     * @param request the transfer request containing serialized entity data
     * @return a future that completes when the pet is spawned
     */
    CompletableFuture<Boolean> receivePet(@NotNull PetTransferRequest request);

    /**
     * Serializes a pet entity for transfer.
     *
     * @param petId the pet entity UUID
     * @return the serialized entity data
     */
    CompletableFuture<SerializedEntity> serializePet(@NotNull UUID petId);

    // ====================================================================================
    // Default implementations delegating to shared EntityTransferService
    // ====================================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation converts the entity transfer to a pet transfer
     * using {@link PetTransferAdapter} patterns. Implementations may override
     * this for platform-specific optimizations.</p>
     */
    @Override
    default CompletableFuture<TransferResult> transferEntity(
        UUID entityUuid,
        String targetServer,
        String targetWorld,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        // Delegate to pet transfer method by default
        // Implementations should override this with proper entity handling
        return transferPet(entityUuid, null, targetServer, targetWorld, x, y, z)
            .thenApply(success -> new TransferResult(
                success,
                entityUuid,
                targetServer,
                success ? Optional.empty() : Optional.of("Pet transfer failed")
            ));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation converts the entity request to a pet request
     * and delegates to {@link #receivePet(PetTransferRequest)}.</p>
     */
    @Override
    default CompletableFuture<UUID> receiveEntity(EntityTransferRequest request) {
        PetTransferRequest petRequest = PetTransferRequest.fromEntityTransferRequest(request);
        return receivePet(petRequest)
            .thenApply(success -> {
                if (success) {
                    return request.entityUuid();
                } else {
                    throw new RuntimeException("Failed to receive pet: " + request.entityUuid());
                }
            });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation delegates to {@link #serializePet(UUID)}.</p>
     */
    @Override
    default CompletableFuture<SerializedEntity> serializeEntity(UUID entityUuid) {
        return serializePet(entityUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation returns true. Implementations should override
     * based on their configuration.</p>
     */
    @Override
    default boolean isTransferEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Default implementation returns an empty string. Implementations must
     * override this to provide the actual local server name.</p>
     */
    @Override
    default String getLocalServerName() {
        return "";
    }
}
