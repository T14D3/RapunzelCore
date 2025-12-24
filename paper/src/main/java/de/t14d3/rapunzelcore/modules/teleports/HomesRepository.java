package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.rapunzelcore.database.entities.Home;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.*;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomesRepository extends EntityRepository<Home> {
    private static final HomesRepository instance = new HomesRepository();      

    private volatile Map<String, List<Home>> homesByPlayer = new LinkedHashMap<>();
    private DbEntitySync.Listener syncListener;
    private final AtomicBoolean reloadQueued = new AtomicBoolean(false);        

    public enum SetHomeStatus {
        CREATED,
        UPDATED,
        LIMIT_REACHED
    }

    public record HomeSnapshot(
        String name,
        String world,
        String server,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
    }

    public record SetHomeResult(SetHomeStatus status, int maxHomes, int remainingHomes) {
    }

    public HomesRepository() {
        super(CoreDatabase.getEntityManager(), Home.class);
    }

    public static HomesRepository getInstance() {
        return instance;
    }

    public static Home getHome(Player bplayer, String name) {
        if (bplayer == null || name == null) return null;
        String needle = name.trim();
        if (needle.isEmpty()) return null;

        List<Home> homes = getHomes(bplayer);
        if (homes == null || homes.isEmpty()) return null;
        return homes.stream()
                .filter(home -> home.getName() != null && home.getName().equalsIgnoreCase(needle))
                .findFirst()
                .orElse(null);
    }

    public static CompletableFuture<HomeSnapshot> getHomeAsync(UUID playerUuid, String name) {
        if (playerUuid == null || name == null) return CompletableFuture.completedFuture(null);
        String needle = name.trim();
        if (needle.isEmpty()) return CompletableFuture.completedFuture(null);

        return CoreDatabase.supplyAsync(() -> {
            PlayerRepository.refreshFromDb(playerUuid);
            PlayerEntity playerEntity = PlayerRepository.getPlayer(playerUuid);
            if (playerEntity == null) return null;
            Home home = playerEntity.getHomes().stream()
                .filter(h -> h != null && h.getName() != null && h.getName().equalsIgnoreCase(needle))
                .findFirst()
                .orElse(null);
            return snapshot(home);
        });
    }

    public static CompletableFuture<List<HomeSnapshot>> getHomesAsync(UUID playerUuid) {
        if (playerUuid == null) return CompletableFuture.completedFuture(List.of());
        return CoreDatabase.supplyAsync(() -> {
            PlayerRepository.refreshFromDb(playerUuid);
            PlayerEntity playerEntity = PlayerRepository.getPlayer(playerUuid);
            if (playerEntity == null) return List.of();
            List<Home> homes = playerEntity.getHomes();
            if (homes == null || homes.isEmpty()) return List.of();
            return homes.stream().map(HomesRepository::snapshot).filter(h -> h != null && h.name() != null).toList();
        });
    }

    public static CompletableFuture<SetHomeResult> setHomeAsync(UUID playerUuid, HomeSnapshot snapshot, int maxHomes) {
        if (playerUuid == null || snapshot == null || snapshot.name() == null) {
            return CompletableFuture.completedFuture(null);
        }
        String homeName = snapshot.name().trim();
        if (homeName.isEmpty()) return CompletableFuture.completedFuture(null);

        int max = maxHomes <= 0 ? 1 : maxHomes;
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            PlayerEntity playerEntity = PlayerRepository.getPlayer(playerUuid);
            if (playerEntity == null) return null;
            List<Home> homes = playerEntity.getHomes();
            if (homes == null) return null;

            Home existing = homes.stream()
                .filter(h -> h != null && h.getName() != null && h.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);

            boolean updating = existing != null;
            if (!updating && homes.size() >= max) {
                return new SetHomeResult(SetHomeStatus.LIMIT_REACHED, max, 0);
            }

            Home home = existing;
            if (home == null) {
                home = new Home();
                home.setName(homeName);
                playerEntity.addHome(home);
            }

            home.setWorld(snapshot.world());
            home.setServer(snapshot.server());
            home.setX(snapshot.x());
            home.setY(snapshot.y());
            home.setZ(snapshot.z());
            home.setYaw(snapshot.yaw());
            home.setPitch(snapshot.pitch());

            instance.save(home);
            // Emit a player upsert event so other nodes refresh player.homes.
            PlayerRepository.getInstance().save(playerEntity);
            CoreDatabase.flushAsync();

            int remaining = max == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, max - homes.size());
            return new SetHomeResult(updating ? SetHomeStatus.UPDATED : SetHomeStatus.CREATED, max, remaining);
        }));
    }

    public static CompletableFuture<Boolean> deleteHomeAsync(UUID playerUuid, String name) {
        if (playerUuid == null || name == null) return CompletableFuture.completedFuture(false);
        String homeName = name.trim();
        if (homeName.isEmpty()) return CompletableFuture.completedFuture(false);

        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            PlayerEntity playerEntity = PlayerRepository.getPlayer(playerUuid);
            if (playerEntity == null) return false;
            List<Home> homes = playerEntity.getHomes();
            if (homes == null) return false;

            Home home = homes.stream()
                .filter(h -> h != null && h.getName() != null && h.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);
            if (home == null) return false;

            homes.remove(home);
            home.setPlayer(null);
            instance.delete(home);
            // Emit a player upsert event so other nodes refresh player.homes.
            PlayerRepository.getInstance().save(playerEntity);
            CoreDatabase.flushAsync();
            return true;
        }));
    }

    public static Home getOrCreateHome(Player bplayer, String name) {
        if (bplayer == null || name == null) return null;
        String homeName = name.trim();
        if (homeName.isEmpty()) return null;

        // Use DB-locked block to create if missing and persist.
        return CoreDatabase.locked(() -> {
            PlayerEntity playerEntity = getDbPlayer(bplayer).orElse(null);
            if (playerEntity == null) return null;

            Home existing = playerEntity.getHomes().stream()
                    .filter(home -> home.getName() != null && home.getName().equalsIgnoreCase(homeName))
                    .findFirst()
                    .orElse(null);
            if (existing != null) return existing;

            Home created = new Home();
            created.setName(homeName);
            // Keep bidirectional relation in sync: player.homes is eagerly fetched.
            playerEntity.addHome(created);
            instance.save(created);
            // Emit a player upsert event so other nodes refresh player.homes.
            PlayerRepository.getInstance().save(playerEntity);

            CoreDatabase.flushAsync();
            return created;
        });
    }

    public static Location getHomeLocation(Player bplayer, String name) {
        Home home = getHome(bplayer, name);
        if (home == null) return null;
        if (!TeleportsNetwork.isLocal(home.getServer())) return null;
        World world = Bukkit.getWorld(home.getWorld());
        if (world == null) return null;
        return new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
    }

    public static List<Home> getHomes(Player bplayer) {
        if (bplayer == null) return List.of();
        PlayerEntity playerEntity = getDbPlayer(bplayer).orElse(null);
        if (playerEntity == null) return List.of();
        List<Home> homes = playerEntity.getHomes();
        return (homes == null) ? List.of() : List.copyOf(homes);
    }

    public static void refreshFromDb(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) return;
        UUID uuid;
        try {
            uuid = UUID.fromString(playerUuid);
        } catch (Exception ignored) {
            return;
        }

        PlayerRepository.refreshFromDb(uuid);
    }

    public static Map<String, Location> getHomeLocations(Player bplayer) {      
        List<Home> homes = getHomes(bplayer);
        if (homes == null) return Map.of();
        Map<String, Location> locations = new HashMap<>();
        for (Home home : homes) {
            World world = Bukkit.getWorld(home.getWorld());
            if (world == null) continue;
            locations.put(home.getName(), new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch()));
        }
        return locations;
    }

    public static void setHome(Player bplayer, String name) {
        if (bplayer == null || name == null) return;
        String homeName = name.trim();
        if (homeName.isEmpty()) return;

        RPlayer player = getServerPlayer(bplayer).orElse(null);
        if (player == null) return;
        RLocation rloc = player.location().orElse(null);
        if (rloc == null) return;
        String worldName = (rloc.world().name() != null && !rloc.world().name().isBlank())
                ? rloc.world().name()
                : rloc.world().identifier();

        final String serverName = TeleportsNetwork.localServerNameIfKnown();

        CoreDatabase.runLocked(() -> {
            PlayerEntity playerEntity = PlayerRepository.getPlayer(player);
            if (playerEntity == null) return;

            Home home = playerEntity.getHomes().stream()
                    .filter(h -> h.getName() != null && h.getName().equalsIgnoreCase(homeName))
                    .findFirst()
                    .orElse(null);
            if (home == null) {
                home = new Home();
                home.setName(homeName);
                playerEntity.addHome(home);
            }

            home.setWorld(worldName);
            home.setServer(serverName);
            home.setX(rloc.x());
            home.setY(rloc.y());
            home.setZ(rloc.z());
            home.setYaw(rloc.yaw());
            home.setPitch(rloc.pitch());

            instance.save(home);
            // Emit a player upsert event so other nodes refresh player.homes.
            PlayerRepository.getInstance().save(playerEntity);
            CoreDatabase.flushAsync();
        });
    }

    public static void deleteHome(Player bplayer, String name) {
        if (bplayer == null || name == null) return;
        String homeName = name.trim();
        if (homeName.isEmpty()) return;

        CoreDatabase.runLocked(() -> {
            PlayerEntity playerEntity = getDbPlayer(bplayer).orElse(null);
            if (playerEntity == null) return;
            Home home = playerEntity.getHomes().stream()
                    .filter(h -> h.getName() != null && h.getName().equalsIgnoreCase(homeName))
                    .findFirst()
                    .orElse(null);
            if (home == null) return;
            playerEntity.getHomes().remove(home);
            home.setPlayer(null);
            instance.delete(home);
            // Emit a player upsert event so other nodes refresh player.homes.
            PlayerRepository.getInstance().save(playerEntity);
            CoreDatabase.flushAsync();
        });
    }

    private void registerSyncListenerIfAvailable() {
        if (syncListener != null) return;
        DbEntitySync sync = CoreDatabase.entitySync();
        if (sync == null) return;
        syncListener = this::onCacheEvent;
        sync.register(syncListener);
    }

    private void onCacheEvent(CacheEvent event, String sourceServer) {
        if (event == null || event.key() == null) return;
        if (!Home.class.getName().equals(event.key().entityClassName())) return;
        queueReloadHomes();
    }

    private void queueReloadHomes() {
        if (!reloadQueued.compareAndSet(false, true)) return;
        CoreDatabase.runAsync(() -> {
            try {
                reloadHomes();
            } finally {
                reloadQueued.set(false);
            }
        });
    }

    private void reloadHomes() {
        homesByPlayer = CoreDatabase.locked(() -> {
            List<Home> list = findAll();
            for (Home home : list) {
                if (home != null) {
                    CoreDatabase.getEntityManager().refresh(home);
                }
            }

            Map<String, List<Home>> map = new LinkedHashMap<>();
            for (Home home : list) {
                if (home == null || home.getPlayer() == null) continue;
                // Try to obtain player's UUID from PlayerEntity; assume getUuid() exists.
                try {
                    Object uuidObj = home.getPlayer().getUuid();
                    if (uuidObj == null) continue;
                    String key = uuidObj.toString();
                    map.computeIfAbsent(key, k -> new ArrayList<>()).add(home);
                } catch (Exception e) {
                    // Fallback: skip homes we cannot key
                }
            }
            return map;
        });
    }

    private static void upsertHomeInCache(Home home) {
        if (home == null || home.getName() == null || home.getPlayer() == null) return;
        // attempt to obtain player's UUID string
        String playerKey;
        try {
            Object uuidObj = home.getPlayer().getUuid();
            if (uuidObj == null) return;
            playerKey = uuidObj.toString();
        } catch (Exception e) {
            return;
        }

        Map<String, List<Home>> updated = new LinkedHashMap<>(instance.homesByPlayer);
        List<Home> list = new ArrayList<>(updated.getOrDefault(playerKey, List.of()));

        // Remove any existing home with same name (case-insensitive), then add
        list.removeIf(h -> h.getName() != null && h.getName().equalsIgnoreCase(home.getName()));
        list.add(home);

        updated.put(playerKey, list);
        instance.homesByPlayer = updated;
    }

    private static void removeHomeFromCache(String playerKey, String name) {
        if (playerKey == null || name == null) return;
        Map<String, List<Home>> updated = new LinkedHashMap<>(instance.homesByPlayer);
        List<Home> list = new ArrayList<>(updated.getOrDefault(playerKey, List.of()));
        boolean changed = list.removeIf(h -> h.getName() != null && h.getName().equalsIgnoreCase(name));
        if (changed) {
            if (list.isEmpty()) {
                updated.remove(playerKey);
            } else {
                updated.put(playerKey, list);
            }
            instance.homesByPlayer = updated;
        }
    }

    private static Optional<RPlayer> getServerPlayer(Player bplayer) {
        if (bplayer == null) return Optional.empty();
        if (!Rapunzel.isBootstrapped()) return Optional.empty();
        Players players = Rapunzel.context().services().get(Players.class);
        return players.wrap(bplayer);
    }

    private static Optional<PlayerEntity> getDbPlayer(Player bplayer) {
        return getServerPlayer(bplayer).map(PlayerRepository::getPlayer);       
    }

    private static HomeSnapshot snapshot(Home home) {
        if (home == null) return null;
        return new HomeSnapshot(
            home.getName(),
            home.getWorld(),
            home.getServer(),
            home.getX(),
            home.getY(),
            home.getZ(),
            home.getYaw(),
            home.getPitch()
        );
    }
}
