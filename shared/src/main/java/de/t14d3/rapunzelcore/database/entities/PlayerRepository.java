package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.repository.EntityRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRepository extends EntityRepository<PlayerEntity> {
    private static final PlayerRepository instance = new PlayerRepository();
    private final Map<String, PlayerEntity> playersByUuid = new ConcurrentHashMap<>();
    private volatile boolean syncRegistered;

    public PlayerRepository() {
        super(CoreDatabase.getEntityManager(), PlayerEntity.class);
        registerSyncListenerIfAvailable();
    }

    public static PlayerRepository getInstance() {
        return instance;
    }

    private void registerSyncListenerIfAvailable() {
        if (syncRegistered) return;
        DbEntitySync sync = CoreDatabase.entitySync();
        if (sync == null) return;
        synchronized (this) {
            if (syncRegistered) return;
            sync.register(this::onCacheEvent);
            syncRegistered = true;
        }
    }

    private void onCacheEvent(CacheEvent event, String sourceServer) {
        if (event == null || event.key() == null) return;
        if (!PlayerEntity.class.getName().equals(event.key().entityClassName())) return;

        String id = event.key().id();
        CacheEvent.Operation operation = event.operation();
        if (id == null || operation == null) return;

        CoreDatabase.runLockedAsync(() -> {
            PlayerEntity cached = playersByUuid.get(id);
            if (cached == null) return;

            if (operation == CacheEvent.Operation.DELETE) {
                playersByUuid.remove(id, cached);
                CoreDatabase.getEntityManager().detach(cached);
                return;
            }
            CoreDatabase.getEntityManager().refresh(cached);
        });
    }

    public static PlayerEntity getPlayer(RPlayer player) {
        if (player == null) return null;
        return getPlayer(player.uuid());
    }

    /**
     * Best-effort helper to map a runtime player into a DB entity.
     * Only works when Rapunzel is bootstrapped and the player is online.
     */
    public static Optional<PlayerEntity> getPlayerFromRuntime(UUID uuid) {
        if (uuid == null) return Optional.empty();
        if (!Rapunzel.isBootstrapped()) return Optional.empty();
        Players players = Rapunzel.context().services().get(Players.class);
        return players.get(uuid).map(PlayerRepository::getPlayer);
    }


    public static PlayerEntity getPlayer(UUID uuid) {
        if (uuid == null) return null;
        instance.registerSyncListenerIfAvailable();

        String id = uuid.toString();
        PlayerEntity cached = instance.playersByUuid.get(id);
        if (cached != null) return cached;

        return CoreDatabase.locked(() -> instance.playersByUuid.computeIfAbsent(id, key -> {
            PlayerEntity playerEntity = instance.findById(key);
            if (playerEntity == null) {
                playerEntity = new PlayerEntity();
                playerEntity.setUuid(uuid);
                instance.save(playerEntity);
                // Ensure concurrent callers see the row and we don't race into duplicate inserts.
                CoreDatabase.getEntityManager().flush();
            }
            return playerEntity;
        }));
    }

    /**
     * Best-effort, join-time resync hook: refreshes a cached {@link PlayerEntity}
     * (including eager relationships like {@code homes}) from the database.
     */
    public static void refreshFromDb(UUID uuid) {
        if (uuid == null) return;
        instance.registerSyncListenerIfAvailable();

        String id = uuid.toString();
        CoreDatabase.runLocked(() -> {
            PlayerEntity playerEntity = instance.playersByUuid.get(id);
            if (playerEntity == null) {
                try {
                    playerEntity = instance.findById(id);
                } catch (Throwable ignored) {
                    return;
                }
                if (playerEntity == null) {
                    playerEntity = new PlayerEntity();
                    playerEntity.setUuid(uuid);
                    try {
                        instance.save(playerEntity);
                        CoreDatabase.getEntityManager().flush();
                    } catch (Throwable ignored) {
                        return;
                    }
                }
                instance.playersByUuid.put(id, playerEntity);
            }

            try {
                CoreDatabase.getEntityManager().refresh(playerEntity);
            } catch (Throwable ignored) {
            }
        });
    }
}
