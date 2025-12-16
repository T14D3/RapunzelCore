package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.entities.Warp;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpsRepository extends EntityRepository<Warp> {
    private static final WarpsRepository instance = new WarpsRepository();

    public WarpsRepository() {
        super(CoreDatabase.getEntityManager(), Warp.class);
    }

    public static WarpsRepository getInstance() {
        return instance;
    }

    public static Warp getWarp(String name) {
        return instance.findOneBy(Map.of("name", name));
    }

    public static Location getWarpLocation(String name) {
        Warp warp = getWarp(name);
        if (warp == null) return null;
        World world = Bukkit.getWorld(warp.getWorld());
        if (world == null) return null;
        return new Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
    }

    public static List<Warp> getWarps() {
        return instance.findAll();
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

    public static void setWarp(String name, Location location, String permission) {
        Warp warp = WarpsRepository.getWarp(name);
        if (warp == null) {
            warp = new Warp();
            warp.setName(name);
        }
        warp.setWorld(location.getWorld().getName());
        warp.setX(location.getX());
        warp.setY(location.getY());
        warp.setZ(location.getZ());
        warp.setYaw(location.getYaw());
        warp.setPitch(location.getPitch());
        warp.setPermission(permission);
        instance.save(warp);
        instance.entityManager.persist(warp);
        instance.entityManager.flush();
    }

    public static void deleteWarp(String name) {
        Warp warp = getWarp(name);
        if (warp != null) {
            instance.deleteById(warp.getId());
            instance.entityManager.flush();
        }
    }

    // Spawn-specific methods using reserved warp names
    private static String getSpawnWarpName(String worldName) {
        return "__internal__" + worldName + "__spawn__";
    }

    public static void setSpawn(String worldName, Location location) {
        String spawnName = getSpawnWarpName(worldName);
        setWarp(spawnName, location, null);
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
}
