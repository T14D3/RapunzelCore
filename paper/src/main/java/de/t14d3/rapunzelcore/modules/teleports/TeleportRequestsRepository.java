package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.TeleportRequest;
import de.t14d3.spool.repository.EntityRepository;

import java.util.Comparator;
import java.util.List;

final class TeleportRequestsRepository extends EntityRepository<TeleportRequest> {
    private static final TeleportRequestsRepository instance = new TeleportRequestsRepository();

    private TeleportRequestsRepository() {
        super(CoreDatabase.getEntityManager(), TeleportRequest.class);
    }

    static TeleportRequestsRepository getInstance() {
        return instance;
    }

    static TeleportRequest create(
        String requesterUuid,
        String requesterName,
        String requesterServer,
        String targetUuid,
        String targetServer,
        boolean tpaHere
    ) {
        return CoreDatabase.locked(() -> {
            TeleportRequest req = new TeleportRequest();
            req.setRequesterUuid(requesterUuid);
            req.setRequesterName(requesterName);
            req.setRequesterServer(requesterServer);
            req.setTargetUuid(targetUuid);
            req.setTargetServer(targetServer);
            req.setTpaHere(tpaHere);
            req.setCreatedAt(System.currentTimeMillis());
            instance.save(req);
            CoreDatabase.getEntityManager().flush();
            return req;
        });
    }

    static List<TeleportRequest> findForTarget(String targetUuid) {
        if (targetUuid == null || targetUuid.isBlank()) return List.of();       
        return CoreDatabase.locked(() -> instance.findBy("targetUuid", targetUuid).stream()
            .sorted(Comparator.comparingLong(TeleportRequest::getCreatedAt).reversed())
            .toList()
        );
    }

    static void delete(long id) {
        CoreDatabase.runLocked(() -> instance.deleteById(id));
        CoreDatabase.flushAsync();
    }
}
