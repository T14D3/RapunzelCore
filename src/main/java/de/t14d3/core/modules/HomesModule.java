package de.t14d3.core.modules;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class HomesModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private NamespacedKey homeKey;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;
        homeKey = new NamespacedKey(plugin, "home");

        registerCommands();
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;

        CommandAPI.unregister("home");
        CommandAPI.unregister("sethome");
        CommandAPI.unregister("delhome");
        enabled = false;
    }

    @Override
    public String getName() {
        return "homes";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void registerCommands() {
        new CommandAPICommand("home")
                .withFullDescription("Teleports the player to their set home location.")
                .withPermission("core.home")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location homeLocation = getHomeLocation(player);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("commands.home.error.no_home"));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(homeLocation);
                    Component message = plugin.getMessage("commands.home.success");
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("sethome")
                .withFullDescription("Sets the player's home location to their current location.")
                .withPermission("core.sethome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    setHomeLocation(player, player.getLocation());
                    Component message = plugin.getMessage("commands.sethome.success")
                            .color(NamedTextColor.GREEN);
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        new CommandAPICommand("delhome")
                .withFullDescription("Deletes the player's set home location.")
                .withPermission("core.delhome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    Location homeLocation = getHomeLocation(player);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("commands.delhome.error.no_home"));
                        return Command.SINGLE_SUCCESS;
                    }
                    setHomeLocation(player, null);
                    player.sendMessage(plugin.getMessage("commands.delhome.success"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    private void setHomeLocation(Player player, Location location) {
        player.getPersistentDataContainer().set(homeKey, PersistentDataType.STRING, locationToString(location));
    }

    private Location getHomeLocation(Player player) {
        String homeData = player.getPersistentDataContainer().get(homeKey, PersistentDataType.STRING);
        if (homeData == null) {
            return null;
        }
        return stringToLocation(homeData);
    }

    private String locationToString(Location location) {
        if (location == null) return null;
        return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location stringToLocation(String data) {
        String[] parts = data.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
    }
}
