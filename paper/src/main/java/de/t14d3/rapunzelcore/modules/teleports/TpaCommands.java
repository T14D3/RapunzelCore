package de.t14d3.rapunzelcore.modules.teleports;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.util.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TpaCommands implements Command {
    private final RapunzelPaperCore plugin;
    private final TpaManager tpaManager;

    public TpaCommands(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        this.tpaManager = new TpaManager(plugin);
        register();
    }

    @Override
    public void register() {

        // tpa command - Request to teleport to a player
        new CommandAPICommand("tpa")
                .withArguments(new EntitySelectorArgument.OnePlayer("target"))
                .withFullDescription("Request to teleport to a player")
                .withPermission("rapunzelcore.tpa")
                .executesPlayer((player, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.self"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (tpaManager.isToggled(target)) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.toggled", target.getName()));
                        return Command.SINGLE_SUCCESS;
                    }

                    tpaManager.createRequest(player, target, false);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.sent", target.getName()));
                    target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.received", player.getName()));
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
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (target.getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.self"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (tpaManager.isToggled(target)) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.toggled", target.getName()));
                        return Command.SINGLE_SUCCESS;
                    }

                    tpaManager.createRequest(player, target, true);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpahere.sent", target.getName()));
                    target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpahere.received", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpaccept command - Accept a teleport request
        new CommandAPICommand("tpaccept")
                .withOptionalArguments(new StringArgument("requester"))
                .withFullDescription("Accept a teleport request")
                .withPermission("rapunzelcore.tpaccept")
                .executesPlayer((player, args) -> {
                    String requesterName = (String) args.getOptional("requester").orElse(null);
                    TpaRequest request = tpaManager.getRequest(player, requesterName);

                    if (request == null) {
                        if (requesterName != null) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.no_request_from", requesterName));
                        } else {
                            List<TpaRequest> requests = tpaManager.getRequests(player);
                            if (requests == null || requests.isEmpty()) {
                                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.no_request"));
                            } else {
                                // Multiple requests, list them
                                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.multiple_requests"));
                                for (TpaRequest req : requests) {
                                    Player reqPlayer = req.getRequester();
                                    if (reqPlayer != null) {
                                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.list_entry", reqPlayer.getName()));
                                    }
                                }
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    }

                    Player requester = request.getRequester();
                    if (requester == null || !requester.isOnline()) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.offline"));
                        tpaManager.removeRequest(player, requesterName);
                        return Command.SINGLE_SUCCESS;
                    }

                    request.teleport();
                    tpaManager.removeRequest(player, requesterName);
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted", requester.getName()));
                    requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted_by", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpdeny command - Deny a teleport request
        new CommandAPICommand("tpdeny")
                .withOptionalArguments(new StringArgument("requester"))
                .withFullDescription("Deny a teleport request")
                .withPermission("rapunzelcore.tpdeny")
                .executesPlayer((player, args) -> {
                    String requesterName = (String) args.getOptional("requester").orElse(null);
                    TpaRequest request = tpaManager.removeRequest(player, requesterName);

                    if (request == null) {
                        if (requesterName != null) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.no_request_from", requesterName));
                        } else {
                            List<TpaRequest> requests = tpaManager.getRequests(player);
                            if (requests == null || requests.isEmpty()) {
                                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.no_request"));
                            } else {
                                // Multiple requests, list them
                                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.multiple_requests"));
                                for (TpaRequest req : requests) {
                                    Player reqPlayer = req.getRequester();
                                    if (reqPlayer != null) {
                                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.list_entry", reqPlayer.getName()));
                                    }
                                }
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    }

                    Player requester = request.getRequester();
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.denied", player.getName()));
                    }
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.denied_request", requester != null ? requester.getName() : "Unknown"));
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
                        plugin.getMessageHandler().getMessage("general.toggle.on") : 
                        plugin.getMessageHandler().getMessage("general.toggle.off");
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tptoggle.toggled", status));
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
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }

                    target.teleport(player.getLocation());
                    player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tphere.success", target.getName()));
                    target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tphere.teleported_by", player.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpo command (admin) - Teleport to a player (supports offline players)
        AsyncPlayerProfileArgument asyncPlayerProfileArgument = new AsyncPlayerProfileArgument("target");
        new CommandAPICommand("tpo")
                .withArguments(asyncPlayerProfileArgument)
                .withFullDescription("Teleport to a player (supports offline players)")
                .withPermission("rapunzelcore.tpo")
                .executesPlayer((player, args) -> {
                    CompletableFuture<List<PlayerProfile>> profileList = args.getByArgument(asyncPlayerProfileArgument);
                    if (profileList == null) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }
                    profileList.thenAccept(profiles -> {
                        if (profiles.isEmpty()) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid"));
                            return;
                        }
                        PlayerProfile profile = profiles.getFirst();
                        if (profile == null || profile.getId() == null) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid"));
                            return;
                        }
                        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(profile.getId());
                        if (!offlineTarget.hasPlayedBefore()) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid"));
                            return;
                        }

                        Location location = offlineTarget.getLocation();
                        if (location != null) {
                            player.teleport(location);
                            player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpo.success", profile.getName()));
                        }

                    }).exceptionally(e -> {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid"));
                        return null;
                    });
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // tpohere command (admin) - Teleport a player to you (supports offline players)
        new CommandAPICommand("tpohere")
                .withArguments(asyncPlayerProfileArgument)
                .withFullDescription("Teleport a player to you (supports offline players)")
                .withPermission("rapunzelcore.tpohere")
                .executesPlayer((player, args) -> {
                    CompletableFuture<List<PlayerProfile>> profileList = args.getByArgument(asyncPlayerProfileArgument);
                    if (profileList == null) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }
                    profileList.thenAccept(profiles -> {
                        if (profiles.isEmpty()) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                            return;
                        }
                        PlayerProfile profile = profiles.getFirst();
                        if (profile == null || profile.getId() == null) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                            return;
                        }
                        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(profile.getId());
                        if (!offlineTarget.hasPlayedBefore()) {
                            player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                            return;
                        }
                        if (offlineTarget.getPlayer() != null) {
                            offlineTarget.getPlayer().teleport(player.getLocation());
                        } else {
                            Utils.setOfflineLocation(profile, player.getLocation());
                        }
                        player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpohere.success", profile.getName()));
                    }).exceptionally(e -> {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        plugin.getLogger().warning("Failed to teleport player to offline location: " + e.getCause().getMessage());
                        return null;
                    });
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }
}
