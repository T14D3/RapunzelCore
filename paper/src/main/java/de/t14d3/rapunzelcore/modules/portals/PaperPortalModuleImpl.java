package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.modules.entitytransfer.EntityTransferModule;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import de.t14d3.rapunzelcore.modules.portals.database.PortalRepository;

/**
 * Paper implementation of PortalModule.
 */
public class PaperPortalModuleImpl implements PortalModule.PortalModuleImpl {
    
    private RapunzelPaperCore core;
    private PaperPortalService portalService;
    private PortalListener portalListener;
    
    @Override
    public void initialize() {
        RapunzelPaperCore core = (RapunzelPaperCore) RapunzelCore.getInstance();
        
        // Initialize portal repository (it registers itself)
        PortalRepository.getInstance();
        
        // Get entity transfer service from module manager
        EntityTransferService entityTransferService = null;
        try {
            EntityTransferModule entityTransferModule = core.getModuleManager().getModule(EntityTransferModule.class);
            if (entityTransferModule != null) {
                entityTransferService = entityTransferModule.getEntityTransferService();
            }
        } catch (Exception e) {
            // Entity transfer module not available
        }
        
        this.core = core;
        this.portalService = new PaperPortalService(core, entityTransferService);
        this.portalListener = new PortalListener(portalService);
    }
    
    @Override
    public void cleanup() {
        if (portalListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(portalListener);
            portalListener = null;
        }
        if (portalService != null) {
            portalService.shutdown();
            portalService = null;
        }
    }
    
    @Override
    public PortalTransferService getPortalTransferService() {
        return portalService;
    }
}
