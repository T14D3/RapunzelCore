package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PendingTeleport;
import de.t14d3.spool.repository.EntityRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class PendingTeleportsRepository extends EntityRepository<PendingTeleport> {
    private static final PendingTeleportsRepository instance = new PendingTeleportsRepository();

    private PendingTeleportsRepository() {
        super(CoreDatabase.getEntityManager(), PendingTeleport.class);
    }

    static PendingTeleportsRepository getInstance() {
        return instance;
    }

    static PendingTeleport create(String playerUuid, String targetServer, String action, String arg) {
        return CoreDatabase.locked(() -> {
            PendingTeleport pending = new PendingTeleport();
            pending.setPlayerUuid(playerUuid);
            pending.setTargetServer(targetServer);
            pending.setAction(action);
            pending.setArg(arg);
            pending.setCreatedAt(System.currentTimeMillis());
            instance.save(pending);
            CoreDatabase.getEntityManager().flush();
            return pending;
        });
    }

    static List<PendingTeleport> findForPlayer(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) return List.of();       
        return CoreDatabase.locked(() -> instance.findBy("playerUuid", playerUuid).stream()
            .sorted(Comparator.comparingLong(PendingTeleport::getCreatedAt).reversed())
            .toList()
        );
    }

    static void delete(long id) {
        CoreDatabase.runLocked(() -> instance.deleteById(id));
        CoreDatabase.flushAsync();
    }
}
