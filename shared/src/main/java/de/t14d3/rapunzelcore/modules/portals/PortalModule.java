package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;

/**
 * Generic PortalModule that delegates to platform-specific implementations.
 * This module provides cross-server portal functionality with customizable areas,
 * particle visuals, and action execution when entities enter.
 */
public class PortalModule implements Module {
    private boolean enabled = false;
    private PortalTransferService portalTransferService;
    private PortalModuleImpl portalImpl;

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        
        // Create platform-specific implementation
        portalImpl = core.getPlatformManager().createPortalModuleImpl(core);
        if (portalImpl != null) {
            portalImpl.initialize();
            this.portalTransferService = portalImpl.getPortalTransferService();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (portalImpl != null) {
            portalImpl.cleanup();
        }
        portalImpl = null;
        portalTransferService = null;
    }

    @Override
    public String getName() {
        return "portals";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER; // Portals are only on backend servers
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the portal transfer service.
     * @return The portal transfer service
     */
    public PortalTransferService getPortalTransferService() {
        return portalTransferService;
    }

    /**
     * Platform-specific implementation interface for portals.
     */
    public interface PortalModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();

        /** Get the portal transfer service. */
        PortalTransferService getPortalTransferService();
    }
}
