package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Home;
import de.t14d3.rapunzelcore.database.entities.Player;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;

public class HomesRepository extends EntityRepository<Home> {
    private static final HomesRepository instance = new HomesRepository();

    public HomesRepository() {
        super(CoreDatabase.getEntityManager(), Home.class);
        super.findAll().forEach(home -> RapunzelCore.getLogger().info("Player: {}", home.getPlayer()));
    }

    public static HomesRepository getInstance() {
        return instance;
    }

    public static Home getHome(org.bukkit.entity.Player bplayer, String name) { 
        if (bplayer == null || name == null) return null;
        String needle = name.trim();
        if (needle.isEmpty()) return null;

        return CoreDatabase.locked(() -> {
            Player player = PlayerRepository.getPlayer(bplayer.getUniqueId());
            if (player == null) return null;
            return player.getHomes().stream()
                .filter(home -> home.getName() != null && home.getName().equalsIgnoreCase(needle))
                .findFirst()
                .orElse(null);
        });
    }

    public static Home getOrCreateHome(org.bukkit.entity.Player bplayer, String name) {
        if (bplayer == null || name == null) return null;
        String homeName = name.trim();
        if (homeName.isEmpty()) return null;

        return CoreDatabase.locked(() -> {
            Player player = PlayerRepository.getPlayer(bplayer.getUniqueId());
            if (player == null) return null;

            Home existing = player.getHomes().stream()
                .filter(home -> home.getName() != null && home.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);
            if (existing != null) return existing;

            Home created = new Home();
            created.setName(homeName);
            created.setPlayer(player);
            instance.save(created);
            CoreDatabase.getEntityManager().flush();
            return created;
        });
    }

    public static Location getHomeLocation(org.bukkit.entity.Player bplayer, String name) {
        Home home = getHome(bplayer, name);
        if (home == null) return null;
        World world = Bukkit.getWorld(home.getWorld());
        if (world == null) return null;
        return new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
    }

    public static List<Home> getHomes(org.bukkit.entity.Player bplayer) {
        if (bplayer == null) return List.of();
        return CoreDatabase.locked(() -> {
            Player player = PlayerRepository.getPlayer(bplayer.getUniqueId());
            if (player == null) return List.of();
            return player.getHomes();
        });
    }

    public static Map<String, Location> getHomeLocations(org.bukkit.entity.Player bplayer) {
        List<Home> homes = getHomes(bplayer);
        if (homes == null) return Map.of();
        Map<String, Location> locations = new HashMap<>();
        for (Home home : homes) {
            World world = Bukkit.getWorld(home.getWorld());
            if (world == null) continue;
            locations.put(home.name, new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch()));
        }
        return locations;
    }

    public static void setHome(org.bukkit.entity.Player bplayer, String name, Location location) {
        if (bplayer == null || name == null || location == null) return;
        String homeName = name.trim();
        if (homeName.isEmpty()) return;

        CoreDatabase.runLocked(() -> {
            Home home = HomesRepository.getOrCreateHome(bplayer, homeName);
            if (home == null) return;
            home.setWorld(location.getWorld().getName());
            home.setX(location.getX());
            home.setY(location.getY());
            home.setZ(location.getZ());
            home.setYaw(location.getYaw());
            home.setPitch(location.getPitch());
            instance.save(home);
            flushAsync();
        });
    }

    public static void deleteHome(org.bukkit.entity.Player bplayer, String name) {
        if (bplayer == null || name == null) return;
        String homeName = name.trim();
        if (homeName.isEmpty()) return;

        CoreDatabase.runLocked(() -> {
            Player player = PlayerRepository.getPlayer(bplayer.getUniqueId());
            if (player == null) return;
            Home home = player.getHomes().stream()
                .filter(h -> h.getName() != null && h.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);
            if (home == null) return;
            instance.deleteById(home.getId());
            flushAsync();
        });
    }
}
