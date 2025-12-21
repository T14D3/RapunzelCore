package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.spool.repository.EntityRepository;

import java.util.Map;
import java.util.UUID;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;

public class PlayerRepository extends EntityRepository<Player> {
    private static final PlayerRepository instance = new PlayerRepository();
    public PlayerRepository() {
        super(CoreDatabase.getEntityManager(), Player.class);
    }

    public static PlayerRepository getInstance() {
        return instance;
    }


    public static Player getPlayer(UUID uuid) {
        return CoreDatabase.locked(() -> {
            Player player = instance.findOneBy(Map.of("uuid", uuid.toString()));
            if (player == null) {
                player = new Player();
                player.setUuid(uuid);
                instance.save(player);
                // Important: flush synchronously so concurrent callers can observe the row
                // and we don't race into duplicate PK inserts.
                CoreDatabase.getEntityManager().flush();
            }
            return player;
        });
    }
}
