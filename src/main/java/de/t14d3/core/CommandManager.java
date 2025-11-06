package de.t14d3.core;

import de.t14d3.core.commands.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

public class CommandManager {
    private final Main plugin;

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
        new GmCommand();
        new GodCommand();
        new HealCommand();
        new HomeCommand(plugin);
        new InvSeeCommand();
        new MsgCommand();
        new OfflineTpCommand();
        new PingCommand();
        new SetHomeCommand(plugin);
        new SocialSpyCommand();
        new SetSpawnCommand(plugin);
        new SpawnCommand(plugin);
        new SpeedCommand();
        new TeamChatCommand();
        new UInfoCommand();
        new VanishCommand();
        new WarpsCommand(plugin);
        new PlaytimeCommand(plugin);
        new RepairCommand(plugin);
        new NickCommand(plugin);
        new ReloadCommand();
    }

    public void setHomeLocation(Player player, Location location) {
        NamespacedKey key = new NamespacedKey(plugin, "home");
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, locationToString(location));
    }

    public Location getHomeLocation(Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "home");
        String homeData = player.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (homeData == null) {
            return null;
        }
        return stringToLocation(homeData);
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location stringToLocation(String data) {
        String[] parts = data.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
    }
}
