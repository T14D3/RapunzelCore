package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class HomeCommand {
    private final FileConfiguration config;

    public HomeCommand(Main plugin) {
        FileConfiguration homes = new YamlConfiguration();
        try {
            homes.load(plugin.getDataFolder().toPath().resolve("homes.yml").toFile());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load homes.yml: " + e.getMessage());
        }
        config = homes;

        new CommandAPICommand("home")
                .withFullDescription("Teleports the player to their set home location.")
                .withPermission("core.home")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location homeLocation = getHomeLocation(player);
                    if (homeLocation == null) {
                        player.sendMessage(Main.getInstance().getMessage("commands.home.error.no_home"));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(homeLocation);
                    Component message = Main.getInstance().getMessage("commands.home.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }

    Location getHomeLocation(Player player) {
        if (!config.isSet("homes." + player.getUniqueId())) {
            return null;
        }
        double x = config.getDouble("homes." + player.getUniqueId() + ".x");
        double y = config.getDouble("homes." + player.getUniqueId() + ".y");
        double z = config.getDouble("homes." + player.getUniqueId() + ".z");
        float yaw = (float) config.getDouble("homes." + player.getUniqueId() + ".yaw");
        float pitch = (float) config.getDouble("homes." + player.getUniqueId() + ".pitch");
        String world = config.getString("homes." + player.getUniqueId() + ".world");
        return new Location(Main.getInstance().getServer().getWorld(world), x, y, z, yaw, pitch);
    }

    public void setHomeLocation(Player player, Location location) {
        config.set("homes." + player.getUniqueId() + ".x", location.getX());
        config.set("homes." + player.getUniqueId() + ".y", location.getY());
        config.set("homes." + player.getUniqueId() + ".z", location.getZ());
        config.set("homes." + player.getUniqueId() + ".yaw", location.getYaw());
        config.set("homes." + player.getUniqueId() + ".pitch", location.getPitch());
        config.set("homes." + player.getUniqueId() + ".world", location.getWorld().getName());
        Main.getInstance().saveConfig();
    }
}
