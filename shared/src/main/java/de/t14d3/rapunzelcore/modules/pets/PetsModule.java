package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;

/**
 * Generic PetsModule that delegates to platform-specific implementations.
 * This module provides cross-server pet transfer functionality.
 */
public class PetsModule implements Module {
    private boolean enabled = false;
    private PetTransferService petTransferService;
    private PetsModuleImpl petsImpl;

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        
        // Create platform-specific implementation
        petsImpl = core.getPlatformManager().createPetsModuleImpl(core);
        if (petsImpl != null) {
            petsImpl.initialize();
            this.petTransferService = petsImpl.getPetTransferService();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (petsImpl != null) {
            petsImpl.cleanup();
        }
        petsImpl = null;
        petTransferService = null;
    }

    @Override
    public String getName() {
        return "pets";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER; // Pets are only on backend servers
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the pet transfer service.
     * @return The pet transfer service
     */
    public PetTransferService getPetTransferService() {
        return petTransferService;
    }

    /**
     * Platform-specific implementation interface for pets.
     */
    public interface PetsModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();

        /** Get the pet transfer service. */
        PetTransferService getPetTransferService();
    }
}
