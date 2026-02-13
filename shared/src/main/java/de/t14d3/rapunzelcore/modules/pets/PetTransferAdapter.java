package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.modules.pets.network.PetTransferRequest;
import de.t14d3.rapunzelcore.network.transfer.AbstractEntityTransferService;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferRequest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter that bridges the pet-specific transfer service with the shared entity transfer service.
 * This class acts as a mediator, converting between pet-specific requests and generic entity
 * transfer requests, enabling pets to use the common cross-server entity transfer infrastructure.
 *
 * <p>The adapter pattern allows the pets module to remain decoupled from the underlying
 * entity transfer implementation while still leveraging the shared service capabilities.</p>
 *
 * @see PetTransferService
 * @see AbstractEntityTransferService
 * @see EntityTransferRequest
 */
public class PetTransferAdapter {

    private static final Logger LOGGER = Logger.getLogger(PetTransferAdapter.class.getName());

    private final AbstractEntityTransferService entityService;
    private final PetTransferService petService;

    /**
     * Creates a new PetTransferAdapter.
     *
     * @param entityService the shared entity transfer service to delegate to
     * @param petService the pet-specific transfer service for handling pet operations
     * @throws NullPointerException if either service is null
     */
    public PetTransferAdapter(
        @NotNull AbstractEntityTransferService entityService,
        @NotNull PetTransferService petService
    ) {
        this.entityService = java.util.Objects.requireNonNull(entityService, "entityService must not be null");
        this.petService = java.util.Objects.requireNonNull(petService, "petService must not be null");
    }

    /**
     * Transfers a pet to another server using the shared entity transfer service.
     * Converts the pet-specific request to a generic entity request and delegates
     * to the entity service for the actual transfer operation.
     *
     * @param request the pet transfer request containing all necessary transfer data
     * @return a CompletableFuture that completes with true if the transfer was initiated
     *         successfully, false otherwise. May complete exceptionally on error.
     */
    public CompletableFuture<Boolean> transferPet(@NotNull PetTransferRequest request) {
        java.util.Objects.requireNonNull(request, "request must not be null");

        LOGGER.fine("Adapting pet transfer request for entity: " + request.petId() +
                   " to server: " + request.targetServer());

        try {
            EntityTransferRequest entityRequest = request.toEntityTransferRequest();
            // Extract parameters from EntityTransferRequest and call transferEntity with correct signature
            return entityService.transferEntity(
                entityRequest.entityUuid(),
                entityRequest.targetServer(),
                entityRequest.targetWorld(),
                entityRequest.targetX(),
                entityRequest.targetY(),
                entityRequest.targetZ(),
                entityRequest.targetYaw(),
                entityRequest.targetPitch()
            ).thenApply(result -> {
                boolean success = result.success();
                if (success) {
                    LOGGER.info("Successfully initiated pet transfer: " + request.petId());
                } else {
                    LOGGER.warning("Pet transfer failed: " + request.petId() +
                                      " - " + result.errorMessage().orElse("Unknown error"));
                }
                return success;
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.SEVERE, "Exception during pet transfer: " + request.petId(), throwable);
                return false;
            });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to adapt pet transfer request: " + request.petId(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Receives a pet transfer from another server.
     * Converts the generic entity request back to a pet-specific request and
     * delegates to the pet service for spawning the pet locally.
     *
     * <p>This method is typically called by the entity transfer service when
     * a pet transfer payload is received from another server.</p>
     *
     * @param entityRequest the entity transfer request received from the network
     * @return a CompletableFuture that completes with true if the pet was received
     *         and spawned successfully, false otherwise. May complete exceptionally on error.
     */
    public CompletableFuture<Boolean> receivePet(@NotNull EntityTransferRequest entityRequest) {
        java.util.Objects.requireNonNull(entityRequest, "entityRequest must not be null");

        LOGGER.fine("Adapting incoming entity transfer to pet request: " + entityRequest.entityUuid());

        try {
            PetTransferRequest petRequest = PetTransferRequest.fromEntityTransferRequest(entityRequest);
            return petService.receivePet(petRequest)
                .thenApply(success -> {
                    if (success) {
                        LOGGER.info("Successfully received pet: " + entityRequest.entityUuid());
                    } else {
                        LOGGER.warning("Failed to receive pet: " + entityRequest.entityUuid());
                    }
                    return success;
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Exception during pet reception: " + entityRequest.entityUuid(), throwable);
                    return false;
                });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to adapt incoming entity request to pet: " + entityRequest.entityUuid(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Initializes the adapter.
     * This method is called during module initialization.
     */
    public void initialize() {
        LOGGER.info("PetTransferAdapter initialized");
    }

    /**
     * Shuts down the adapter.
     * This method is called during module shutdown.
     */
    public void shutdown() {
        LOGGER.info("PetTransferAdapter shut down");
    }

    /**
     * Gets the underlying entity transfer service.
     *
     * @return the entity transfer service
     */
    public AbstractEntityTransferService getEntityService() {
        return entityService;
    }

    /**
     * Gets the underlying pet transfer service.
     *
     * @return the pet transfer service
     */
    public PetTransferService getPetService() {
        return petService;
    }
}
