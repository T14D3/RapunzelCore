package de.t14d3.core;

import de.t14d3.core.commands.*;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.nio.file.Files;

public class CommandManager {
    private final Main plugin;
    private final FileConfiguration homes;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        new AnvilCommand();
        new BackCommand();
        new BroadcastCommand();
        new CraftCommand();
        new DelHomeCommand(plugin);
        new EnderChestCommand();
        new FlyCommand();
        new FlySpeedCommand();
        new HomeCommand(plugin);
        new InvSeeCommand();
        new MsgCommand();
        new OfflineTpCommand();
        new SetHomeCommand(plugin);
        new SocialSpyCommand();
        new SetSpawnCommand(plugin);
        new SpawnCommand(plugin);
        new SpeedCommand();
        new TeamChatCommand();
        new UInfoCommand();
        new VanishCommand();

        FileConfiguration homes = new YamlConfiguration();
        try {
            homes.load(plugin.getDataFolder().toPath().resolve("homes.yml").toFile());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load homes.yml: " + e.getMessage());
            try {
                Files.createFile(plugin.getDataFolder().toPath().resolve("homes.yml"));
            } catch (Exception e1) {
                plugin.getLogger().warning("Failed to create homes.yml: " + e1.getMessage());
            }
        }
        this.homes = homes;

    }

    public FileConfiguration getHomes() {
        return homes;
    }

    public void setHomeLocation(Player player, Location location) {
        homes.set("homes." + player.getUniqueId() + ".x", location.getX());
        homes.set("homes." + player.getUniqueId() + ".y", location.getY());
        homes.set("homes." + player.getUniqueId() + ".z", location.getZ());
        homes.set("homes." + player.getUniqueId() + ".yaw", location.getYaw());
        homes.set("homes." + player.getUniqueId() + ".pitch", location.getPitch());
        homes.set("homes." + player.getUniqueId() + ".world", location.getWorld().getName());
        saveHomes();
    }

    public Location getHomeLocation(Player player) {
        if (!homes.isSet("homes." + player.getUniqueId())) {
            return null;
        }
        double x = homes.getDouble("homes." + player.getUniqueId() + ".x");
        double y = homes.getDouble("homes." + player.getUniqueId() + ".y");
        double z = homes.getDouble("homes." + player.getUniqueId() + ".z");
        float yaw = (float) homes.getDouble("homes." + player.getUniqueId() + ".yaw");
        float pitch = (float) homes.getDouble("homes." + player.getUniqueId() + ".pitch");
        String world = homes.getString("homes." + player.getUniqueId() + ".world");
        return new Location(Main.getInstance().getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    public void saveHomes() {
        try {
            homes.save(plugin.getDataFolder().toPath().resolve("homes.yml").toFile());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save homes.yml: " + e.getMessage());
        }
    }


}
