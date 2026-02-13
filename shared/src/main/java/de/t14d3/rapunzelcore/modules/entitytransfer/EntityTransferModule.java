package de.t14d3.rapunzelcore.modules.entitytransfer;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;

/**
 * Generic EntityTransferModule that delegates to platform-specific implementations.
 * This module provides cross-server entity transfer functionality.
 */
public class EntityTransferModule implements Module {
    private boolean enabled = false;
    private EntityTransferService entityTransferService;
    private EntityTransferModuleImpl entityTransferImpl;

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        
        // Create platform-specific implementation
        entityTransferImpl = core.getPlatformManager().createEntityTransferModuleImpl(core);
        if (entityTransferImpl != null) {
            entityTransferImpl.initialize();
            this.entityTransferService = entityTransferImpl.getEntityTransferService();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (entityTransferImpl != null) {
            entityTransferImpl.cleanup();
        }
        entityTransferImpl = null;
        entityTransferService = null;
    }

    @Override
    public String getName() {
        return "entitytransfer";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER; // Entity transfer is only on backend servers
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the entity transfer service.
     * @return The entity transfer service
     */
    public EntityTransferService getEntityTransferService() {
        return entityTransferService;
    }

    /**
     * Platform-specific implementation interface for entity transfer.
     */
    public interface EntityTransferModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();

        /** Get the entity transfer service. */
        EntityTransferService getEntityTransferService();
    }
}
