package de.t14d3.rapunzelcore.modules.teleports;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class TpaCommands {
    private final Main plugin;
    private final TpaManager tpaManager;

    public TpaCommands(Main plugin) {
        this.plugin = plugin;
        this.tpaManager = new TpaManager(plugin);
    }

    public void register() {
        // Schedule cleanup task
        Bukkit.getScheduler().runTaskTimer(plugin, tpaManager::cleanup, 6000L, 6000L); // Run every 5 minutes

        // tpa command - Request to teleport to a player
        new CommandAPICommand("tpa")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withFullDescription("Request to teleport to a player")
                .withPermission("rapunzelcore.tpa")
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null) {
                        player.sendMessage(plugin.getMessage("error.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.getMessage("teleports.tpa.error.self"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (tpaManager.isToggled(target)) {
                        player.sendMessage(plugin.getMessage("teleports.tpa.error.toggled", target.getName()));
                        return Command.SINGLE_SUCCESS;
                    }

                    tpaManager.createRequest(player, target, false);
                    player.sendMessage(plugin.getMessage("teleports.tpa.sent", target.getName()));
                    target.sendMessage(plugin.getMessage("teleports.tpa.received", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpahere command - Request a player to teleport to you
        new CommandAPICommand("tpahere")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withFullDescription("Request a player to teleport to you")
                .withPermission("rapunzelcore.tpahere")
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null) {
                        player.sendMessage(plugin.getMessage("error.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.getMessage("teleports.tpa.error.self"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (tpaManager.isToggled(target)) {
                        player.sendMessage(plugin.getMessage("teleports.tpa.error.toggled", target.getName()));
                        return Command.SINGLE_SUCCESS;
                    }

                    tpaManager.createRequest(player, target, true);
                    player.sendMessage(plugin.getMessage("teleports.tpahere.sent", target.getName()));
                    target.sendMessage(plugin.getMessage("teleports.tpahere.received", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpaccept command - Accept a teleport request
        new CommandAPICommand("tpaccept")
                .withFullDescription("Accept a teleport request")
                .withPermission("rapunzelcore.tpaccept")
                .executesPlayer((player, args) -> {
                    TpaRequest request = tpaManager.getRequest(player);
                    if (request == null) {
                        player.sendMessage(plugin.getMessage("teleports.tpaccept.error.no_request"));
                        return Command.SINGLE_SUCCESS;
                    }

                    Player requester = request.getRequester();
                    if (requester == null || !requester.isOnline()) {
                        player.sendMessage(plugin.getMessage("error.player_offline"));
                        tpaManager.removeRequest(player);
                        return Command.SINGLE_SUCCESS;
                    }

                    request.teleport(player.getUniqueId());
                    tpaManager.removeRequest(player);
                    player.sendMessage(plugin.getMessage("teleports.tpaccept.accepted", requester.getName()));
                    requester.sendMessage(plugin.getMessage("teleports.tpaccept.accepted_by", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpdeny command - Deny a teleport request
        new CommandAPICommand("tpdeny")
                .withFullDescription("Deny a teleport request")
                .withPermission("rapunzelcore.tpdeny")
                .executesPlayer((player, args) -> {
                    TpaRequest request = tpaManager.removeRequest(player);
                    if (request == null) {
                        player.sendMessage(plugin.getMessage("teleports.tpdeny.error.no_request"));
                        return Command.SINGLE_SUCCESS;
                    }

                    Player requester = request.getRequester();
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage(plugin.getMessage("teleports.tpdeny.denied", player.getName()));
                    }
                    player.sendMessage(plugin.getMessage("teleports.tpdeny.denied_request", requester != null ? requester.getName() : "Unknown"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tptoggle command - Toggle teleport requests
        new CommandAPICommand("tptoggle")
                .withFullDescription("Toggle teleport requests on/off")
                .withPermission("rapunzelcore.tptoggle")
                .executesPlayer((player, args) -> {
                    boolean toggled = !tpaManager.isToggled(player);
                    tpaManager.setToggled(player, toggled);
                    Component status = toggled ? 
                        plugin.getMessage("general.toggle.on") : 
                        plugin.getMessage("general.toggle.off");
                    player.sendMessage(plugin.getMessages().getMessage("teleports.tptoggle.toggled", status));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tphere command (admin) - Teleport a player to you
        new CommandAPICommand("tphere")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withFullDescription("Teleport a player to you")
                .withPermission("rapunzelcore.tphere")
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null) {
                        player.sendMessage(plugin.getMessage("error.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }

                    target.teleport(player.getLocation());
                    player.sendMessage(plugin.getMessage("teleports.tphere.success", target.getName()));
                    target.sendMessage(plugin.getMessage("teleports.tphere.teleported_by", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpo command (admin) - Teleport to a player (supports offline players)
        new CommandAPICommand("tpo")
                .withArguments(new StringArgument("target"))
                .withFullDescription("Teleport to a player (supports offline players)")
                .withPermission("rapunzelcore.tpo")
                .executesPlayer((player, args) -> {
                    String targetName = (String) args.get("target");
                    
                    // Try to get online player first
                    Player onlineTarget = Bukkit.getPlayer(targetName);
                    if (onlineTarget != null) {
                        player.teleport(onlineTarget.getLocation());
                        player.sendMessage(plugin.getMessage("teleports.tpo.success", onlineTarget.getName()));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    // Handle offline player
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    if (!offlineTarget.hasPlayedBefore()) {
                        player.sendMessage(plugin.getMessage("error.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    // Check if offline player has a bed spawn location
                    org.bukkit.Location bedSpawn = offlineTarget.getBedSpawnLocation();
                    if (bedSpawn != null) {
                        player.teleport(bedSpawn);
                        player.sendMessage(plugin.getMessage("teleports.tpo.success_offline", targetName));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    // Fallback to world spawn
                    org.bukkit.Location worldSpawn = offlineTarget.getLocation() != null ? 
                        offlineTarget.getLocation() : 
                        Bukkit.getWorlds().get(0).getSpawnLocation();
                    
                    player.teleport(worldSpawn);
                    player.sendMessage(plugin.getMessage("teleports.tpo.success_offline", targetName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpohere command (admin) - Teleport a player to you (supports offline players)
        new CommandAPICommand("tpohere")
                .withArguments(new StringArgument("target"))
                .withFullDescription("Teleport a player to you (supports offline players)")
                .withPermission("rapunzelcore.tpohere")
                .executesPlayer((player, args) -> {
                    String targetName = (String) args.get("target");
                    
                    // Try to get online player first
                    Player onlineTarget = Bukkit.getPlayer(targetName);
                    if (onlineTarget != null) {
                        onlineTarget.teleport(player.getLocation());
                        player.sendMessage(plugin.getMessage("teleports.tpohere.success", onlineTarget.getName()));
                        onlineTarget.sendMessage(plugin.getMessage("teleports.tpohere.teleported_by", player.getName()));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    // Handle offline player - cannot teleport offline players, but show message
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    if (!offlineTarget.hasPlayedBefore()) {
                        player.sendMessage(plugin.getMessage("error.player_not_found"));
                        return Command.SINGLE_SUCCESS;
                    }
                    
                    player.sendMessage(plugin.getMessage("teleports.tpohere.error.offline", targetName));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }
}
