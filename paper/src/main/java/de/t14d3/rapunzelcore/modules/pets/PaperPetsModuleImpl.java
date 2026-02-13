package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.modules.pets.PetRepository;
import de.t14d3.rapunzelcore.modules.entitytransfer.EntityTransferModule;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;

/**
 * Paper implementation of PetsModule.
 */
public class PaperPetsModuleImpl implements PetsModule.PetsModuleImpl {
    
    private PaperPetTransferService petTransferService;
    
    @Override
    public void initialize() {
        RapunzelPaperCore core = (RapunzelPaperCore) RapunzelCore.getInstance();
        
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
        
        // Get pet repository singleton
        PetRepository petRepository = PetRepository.getInstance();
        
        this.petTransferService = new PaperPetTransferService(core, entityTransferService, petRepository);
    }
    
    @Override
    public void cleanup() {
        if (petTransferService != null) {
            petTransferService.shutdown();
            petTransferService = null;
        }
    }
    
    @Override
    public PetTransferService getPetTransferService() {
        return petTransferService;
    }
}
