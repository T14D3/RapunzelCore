package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.entities.Home;
import de.t14d3.rapunzelcore.entities.Player;
import de.t14d3.rapunzelcore.entities.PlayerRepository;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomesRepository extends EntityRepository<Home> {
    private static final HomesRepository instance = new HomesRepository();

    public HomesRepository() {
        super(CoreDatabase.getEntityManager(), Home.class);
        super.findAll().forEach(home -> Main.getInstance().getLogger().info("Player: " + home.getPlayer()));
    }

    public static HomesRepository getInstance() {
        return instance;
    }

    public static Home getHome(org.bukkit.entity.Player bplayer, String name) {
        Player player = PlayerRepository.getPlayer(bplayer);
        if (player == null) return null;
        return instance.findOneBy(Map.of("player", player, "name", name));
    }

    public static Location getHomeLocation(org.bukkit.entity.Player bplayer, String name) {
        Home home = getHome(bplayer, name);
        if (home == null) return null;
        World world = Bukkit.getWorld(home.getWorld());
        if (world == null) return null;
        return new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
    }

    public static List<Home> getHomes(org.bukkit.entity.Player bplayer) {
        Player player = PlayerRepository.getPlayer(bplayer);
        if (player == null) return List.of();
        return player.getHomes();
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
        Player player = PlayerRepository.getPlayer(bplayer);
        if (player == null) return;
        Home home = HomesRepository.getHome(bplayer, name);
        if (home == null) {
            home = new Home();
            home.setName(name);
            player.addHome(home);
            instance.entityManager.persist(player);
            instance.entityManager.flush();
        }
        instance.entityManager.refresh(home);
        home.setWorld(location.getWorld().getName());
        home.setX(location.getX());
        home.setY(location.getY());
        home.setZ(location.getZ());
        home.setYaw(location.getYaw());
        home.setPitch(location.getPitch());
        instance.save(home);
        instance.entityManager.flush();
    }

    public static void deleteHome(org.bukkit.entity.Player bplayer, String name) {
        Player player = PlayerRepository.getPlayer(bplayer);
        if (player == null) return;
        HomesRepository.getHome(bplayer, name).setPlayer(null);
        instance.deleteById(HomesRepository.getHome(bplayer, name).getId());
        instance.entityManager.flush();
    }
}
