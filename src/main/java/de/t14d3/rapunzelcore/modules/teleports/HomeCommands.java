package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.entities.Home;
import de.t14d3.rapunzelcore.modules.commands.Command;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeCommands implements Command {
    private final Main plugin;

    public HomeCommands(Main plugin) {
        this.plugin = plugin;
    }

    private int getMaxHomes(Player player) {
        if (player.hasPermission("rapunzelcore.homes.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("rapunzelcore.homes." + i)) {
                return i;
            }
        }
        return 1; // Default to 1 home if no specific permission is found
    }

    private boolean canSetMoreHomes(Player player) {
        int currentHomes = HomesRepository.getHomes(player).size();
        int maxHomes = getMaxHomes(player);
        return currentHomes < maxHomes;
    }

    public void register() {
        // Home command
        new CommandAPICommand("home")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Teleports the player to a home location.")
                .withPermission("rapunzelcore.home")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    player.sendMessage(String.valueOf(HomesRepository.getInstance().findAll().size()));
                    Location homeLocation = HomesRepository.getHomeLocation(player, homeName);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("teleports.home.error.no_home", homeName));
                        return Command.SINGLE_SUCCESS;
                    }
                    player.teleport(homeLocation);
                    player.sendMessage(plugin.getMessage("teleports.home.success", homeName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Sethome command
        new CommandAPICommand("sethome")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Sets the player's home location.")
                .withPermission("rapunzelcore.sethome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    
                    // Check if player already has a home with this name (updating is always allowed)
                    boolean isUpdating = HomesRepository.getHomeLocation(player, homeName) != null;
                    
                    // If not updating, check if they can set more homes
                    if (!isUpdating && !canSetMoreHomes(player)) {
                        int maxHomes = getMaxHomes(player);
                        player.sendMessage(plugin.getMessage("teleports.sethome.error.limit_reached", 
                                String.valueOf(maxHomes)).color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    HomesRepository.setHome(player, homeName, player.getLocation());
                    
                    if (isUpdating) {
                        player.sendMessage(plugin.getMessage("teleports.sethome.updated", homeName).color(NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(plugin.getMessage("teleports.sethome.success", homeName).color(NamedTextColor.GREEN));
                        
                        // Show remaining homes if not unlimited
                        if (!player.hasPermission("rapunzelcore.homes.unlimited")) {
                            int remaining = getMaxHomes(player) - HomesRepository.getHomes(player).size();
                            if (remaining == 0) {
                                player.sendMessage(plugin.getMessage("teleports.sethome.limit_reached")
                                        .color(NamedTextColor.GOLD));
                            } else {
                                player.sendMessage(plugin.getMessage("teleports.sethome.remaining", 
                                        String.valueOf(remaining)).color(NamedTextColor.GRAY));
                            }
                        }
                    }

                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Delhome command
        new CommandAPICommand("delhome")
                .withArguments(new StringArgument("homeName").setOptional(true))
                .withFullDescription("Deletes the player's home location.")
                .withPermission("rapunzelcore.delhome")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    String homeName = (String) args.get("homeName");
                    if (homeName == null) {
                        homeName = "default";
                    }
                    Location homeLocation = HomesRepository.getHomeLocation(player, homeName);
                    if (homeLocation == null) {
                        player.sendMessage(plugin.getMessage("teleports.delhome.error.no_home", homeName));
                        return Command.SINGLE_SUCCESS;
                    }
                    HomesRepository.deleteHome(player, homeName);
                    player.sendMessage(plugin.getMessage("teleports.delhome.success", homeName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // Homes command
        new CommandAPICommand("homes")
                .withFullDescription("Lists all your home locations.")
                .withPermission("rapunzelcore.homes")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    List<Home> homes = HomesRepository.getHomes(player);
                    if (homes.isEmpty()) {
                        player.sendMessage(plugin.getMessage("teleports.homes.error.none"));
                        return Command.SINGLE_SUCCESS;
                    }
                    Component message = plugin.getMessage("teleports.homes.header");
                    for (Home home : homes) {
                        Location loc = home.getLocation();
                        message = message.appendNewline().append(plugin.getMessage("teleports.homes.entry",
                                home.getName(),
                                String.valueOf(loc.getBlockX()),
                                String.valueOf(loc.getBlockY()),
                                String.valueOf(loc.getBlockZ())
                        ));
                    }
                    player.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    @Override
    public void unregister() {
        List.of("home", "sethome", "delhome", "homes").forEach(cmd -> CommandAPI.unregister(cmd));
    }
}
