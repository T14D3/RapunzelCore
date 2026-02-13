package de.t14d3.rapunzelcore.modules.entitytransfer;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import de.t14d3.rapunzelcore.services.entitytransfer.PaperEntityTransferService;

/**
 * Paper implementation of EntityTransferModule.
 */
public class PaperEntityTransferModuleImpl implements EntityTransferModule.EntityTransferModuleImpl {
    
    private PaperEntityTransferService entityTransferService;
    
    @Override
    public void initialize() {
        RapunzelPaperCore core = (RapunzelPaperCore) RapunzelCore.getInstance();
        this.entityTransferService = new PaperEntityTransferService(core);
    }
    
    @Override
    public void cleanup() {
        if (entityTransferService != null) {
            entityTransferService.shutdown();
            entityTransferService = null;
        }
    }
    
    @Override
    public EntityTransferService getEntityTransferService() {
        return entityTransferService;
    }
}
