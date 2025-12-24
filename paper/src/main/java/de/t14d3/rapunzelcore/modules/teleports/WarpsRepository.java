package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.rapunzelcore.database.entities.Warp;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RPlayer;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;

public class WarpsRepository extends EntityRepository<Warp> {
    private static final WarpsRepository instance = new WarpsRepository();      

    private volatile Map<String, Warp> warpsByName = new LinkedHashMap<>();     
    private DbEntitySync.Listener syncListener;
    private final AtomicBoolean reloadQueued = new AtomicBoolean(false);

    public WarpsRepository() {
        super(CoreDatabase.getEntityManager(), Warp.class);
        reloadWarps();
        registerSyncListenerIfAvailable();
    }

    public static WarpsRepository getInstance() {
        return instance;
    }

    public static Warp getWarp(String name) {
        if (name == null || name.isBlank()) return null;
        return instance.warpsByName.get(name.toLowerCase());
    }

    public static Location getWarpLocation(String name) {
        Warp warp = getWarp(name);
        if (warp == null) return null;
        if (!TeleportsNetwork.isLocal(warp.getServer())) return null;
        World world = Bukkit.getWorld(warp.getWorld());
        if (world == null) return null;
        return new Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
    }

    public static List<Warp> getWarps() {
        return List.copyOf(instance.warpsByName.values());
    }

    public static Map<String, Location> getWarpLocations() {
        List<Warp> warps = getWarps();
        Map<String, Location> locations = new HashMap<>();
        for (Warp warp : warps) {
            World world = Bukkit.getWorld(warp.getWorld());
            if (world == null) continue;
            locations.put(warp.getName(), new Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch()));
        }
        return locations;
    }

    public static void setWarp(Player bplayer, String name, String permission) {
        if (bplayer == null || name == null || name.isBlank()) return;
        String warpName = name.trim();
        if (warpName.isEmpty()) return;

        RPlayer player = Rapunzel.players().get(bplayer.getUniqueId()).orElse(null);
        if (player == null) return;

        RLocation rloc = player.location().orElse(null);
        if (rloc == null) return;
        String worldName = (rloc.world().name() != null && !rloc.world().name().isBlank())
            ? rloc.world().name()
            : rloc.world().identifier();
        String serverName = resolveServerName();

        CoreDatabase.runLocked(() -> {
            Warp warp = instance.warpsByName.get(warpName.toLowerCase());       
            if (warp == null) {
                warp = instance.findOneBy("name", warpName);
            }
            if (warp == null) {
                warp = new Warp();
                warp.setName(warpName);
            }

            warp.setWorld(worldName);
            warp.setServer(serverName);
            warp.setX(rloc.x());
            warp.setY(rloc.y());
            warp.setZ(rloc.z());
            warp.setYaw(rloc.yaw());
            warp.setPitch(rloc.pitch());
            warp.setPermission(permission);

            instance.save(warp);
            upsertWarpInCache(warp);
            flushAsync();
        });
    }

    public static CompletableFuture<Void> setWarpAsync(
        String name,
        String worldName,
        String serverName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String permission
    ) {
        if (name == null || name.isBlank()) return CompletableFuture.completedFuture(null);
        String warpName = name.trim();
        if (warpName.isEmpty()) return CompletableFuture.completedFuture(null);

        return CoreDatabase.runLockedAsync(() -> {
            Warp warp = instance.warpsByName.get(warpName.toLowerCase());
            if (warp == null) {
                warp = instance.findOneBy("name", warpName);
            }
            if (warp == null) {
                warp = new Warp();
                warp.setName(warpName);
            }

            warp.setWorld(worldName);
            warp.setServer(serverName);
            warp.setX(x);
            warp.setY(y);
            warp.setZ(z);
            warp.setYaw(yaw);
            warp.setPitch(pitch);
            warp.setPermission(permission);

            instance.save(warp);
            upsertWarpInCache(warp);
            flushAsync();
        });
    }

    public static CompletableFuture<Boolean> deleteWarpAsync(String name) {
        if (name == null || name.isBlank()) return CompletableFuture.completedFuture(false);
        String warpName = name.trim();
        if (warpName.isEmpty()) return CompletableFuture.completedFuture(false);

        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            Warp warp = instance.warpsByName.get(warpName.toLowerCase());
            if (warp == null) {
                warp = instance.findOneBy("name", warpName);
            }
            if (warp == null) return false;

            instance.delete(warp);
            removeWarpFromCache(warpName);
            flushAsync();
            return true;
        }));
    }

    // Spawn-specific methods using reserved warp names
    private static String getSpawnWarpName(String worldName) {
        return "__internal__" + worldName + "__spawn__";
    }

    public static void setSpawn(Player bplayer) {
        if (bplayer == null) return;
        RPlayer player = Rapunzel.players().get(bplayer.getUniqueId()).orElse(null);
        if (player == null) return;
        RLocation rloc = player.location().orElse(null);
        if (rloc == null) return;
        String worldName = (rloc.world().name() != null && !rloc.world().name().isBlank())
            ? rloc.world().name()
            : rloc.world().identifier();
        String spawnName = getSpawnWarpName(worldName);
        setWarp(bplayer, spawnName, null);
    }

    public static Location getSpawn(String worldName) {
        String spawnName = getSpawnWarpName(worldName);
        return getWarpLocation(spawnName);
    }

    public static Location getSpawnLocation(String worldName) {
        return getSpawn(worldName);
    }

    public static List<Warp> getPublicWarps() {
        List<Warp> warps = getWarps();
        List<Warp> publicWarps = new ArrayList<>();
        for (Warp warp : warps) {
            String name = warp.getName();
            if (!(name.startsWith("__internal__") && name.endsWith("__spawn__"))) {
                publicWarps.add(warp);
            }
        }
        return publicWarps;
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
        if (!Warp.class.getName().equals(event.key().entityClassName())) return;
        queueReloadWarps();
    }

    private void queueReloadWarps() {
        if (!reloadQueued.compareAndSet(false, true)) return;
        CoreDatabase.runAsync(() -> {
            try {
                reloadWarps();
            } finally {
                reloadQueued.set(false);
            }
        });
    }

    private void reloadWarps() {
        warpsByName = CoreDatabase.locked(() -> {
            List<Warp> list = findAll();
            for (Warp warp : list) {
                if (warp != null) {
                    CoreDatabase.getEntityManager().refresh(warp);
                }
            }

            Map<String, Warp> map = new LinkedHashMap<>();
            for (Warp warp : list) {
                if (warp == null || warp.getName() == null) continue;
                map.put(warp.getName().toLowerCase(), warp);
            }
            return map;
        });
    }

    private static void upsertWarpInCache(Warp warp) {
        if (warp == null || warp.getName() == null) return;
        String key = warp.getName().toLowerCase();
        Map<String, Warp> updated = new LinkedHashMap<>(instance.warpsByName);
        updated.put(key, warp);
        instance.warpsByName = updated;
    }

    private static void removeWarpFromCache(String name) {
        if (name == null) return;
        String key = name.toLowerCase();
        Map<String, Warp> updated = new LinkedHashMap<>(instance.warpsByName);  
        updated.remove(key);
        instance.warpsByName = updated;
    }

    private static String resolveServerName() {
        return TeleportsNetwork.localServerNameIfKnown();
    }
}
