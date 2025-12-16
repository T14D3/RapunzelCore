package de.t14d3.rapunzelcore.entities;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.spool.repository.EntityRepository;

import java.util.Map;

public class PlayerRepository extends EntityRepository<Player> {
    private static final PlayerRepository instance = new PlayerRepository();
    public PlayerRepository() {
        super(CoreDatabase.getEntityManager(), Player.class);
    }

    public static PlayerRepository getInstance() {
        return instance;
    }

    public static Player getPlayer(org.bukkit.entity.Player bplayer) {
        Player player = instance.findOneBy(Map.of("uuid", bplayer.getUniqueId()));
        if (player == null) {
            player = new Player();
            player.setUuid(bplayer.getUniqueId());
            instance.save(player);
            instance.entityManager.flush();
        }
        return player;
    }
}
