package de.t14d3.rapunzelcore.modules.teleports;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.rapunzelcore.database.entities.TeleportRequest;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.modules.teleports.network.NotifyPlayerMessage;
import de.t14d3.rapunzelcore.modules.teleports.network.ProxyConnectRequest;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.util.Utils;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.info.NetworkPlayerInfo;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;

public class TpaCommands implements Command {
    private static final long REQUEST_EXPIRE_MS = 60_000L;

    private final RapunzelPaperCore plugin;

    public TpaCommands(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public void register() {
        new CommandAPICommand("tpa")
            .withArguments(new StringArgument("target"))
            .withFullDescription("Request to teleport to a player (cross-server)")
            .withPermission("rapunzelcore.tpa")
            .executesPlayer((player, args) -> {
                String targetName = (String) args.get("target");
                createRequest(player, targetName, false);
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        new CommandAPICommand("tpahere")
            .withArguments(new StringArgument("target"))
            .withFullDescription("Request a player to teleport to you (cross-server)")
            .withPermission("rapunzelcore.tpahere")
            .executesPlayer((player, args) -> {
                String targetName = (String) args.get("target");
                createRequest(player, targetName, true);
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        new CommandAPICommand("tpaccept")
            .withOptionalArguments(new StringArgument("requester"))
            .withFullDescription("Accept a teleport request")
            .withPermission("rapunzelcore.tpaccept")
            .executesPlayer((player, args) -> {
                String requesterName = (String) args.getOptional("requester").orElse(null);
                acceptRequest(player, requesterName);
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        new CommandAPICommand("tpdeny")
            .withOptionalArguments(new StringArgument("requester"))
            .withFullDescription("Deny a teleport request")
            .withPermission("rapunzelcore.tpdeny")
            .executesPlayer((player, args) -> {
                String requesterName = (String) args.getOptional("requester").orElse(null);
                denyRequest(player, requesterName);
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        new CommandAPICommand("tptoggle")
            .withFullDescription("Toggle teleport requests on/off")
            .withPermission("rapunzelcore.tptoggle")
            .executesPlayer((player, args) -> {
                boolean enabled = isToggled(player);
                setToggled(player, !enabled);
                Component state = !enabled
                    ? plugin.getMessageHandler().getMessage("general.toggle.on")
                    : plugin.getMessageHandler().getMessage("general.toggle.off");
                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tptoggle.toggled", state));
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        new CommandAPICommand("tphere")
            .withArguments(new StringArgument("target"))
            .withFullDescription("Teleport a player to you (same server)")
            .withPermission("rapunzelcore.tphere")
            .executesPlayer((player, args) -> {
                String targetName = (String) args.get("target");
                Player target = Bukkit.getPlayerExact(targetName);
                if (target == null) {
                    player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", targetName));
                    return Command.SINGLE_SUCCESS;
                }
                target.teleport(player.getLocation());
                player.sendMessage(plugin.getMessageHandler().getMessage("teleports.tphere.success", target.getName()));
                target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tphere.teleported_by", player.getName()));
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

        // Admin: /tpo and /tpohere (offline supported) - unchanged
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
                profileList.thenAccept(profiles -> Bukkit.getScheduler().runTask(plugin, () -> {
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
                })).exceptionally(e -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid"))
                    );
                    return null;
                });
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);

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
                profileList.thenAccept(profiles -> Bukkit.getScheduler().runTask(plugin, () -> {
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
                })).exceptionally(e -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        plugin.getLogger().warning("Failed to teleport player to offline location: " + e.getCause().getMessage());
                    });
                    return null;
                });
                return Command.SINGLE_SUCCESS;
            })
            .register(plugin);
    }

    @Override
    public void unregister() {
        List.of("tpa", "tpahere", "tpaccept", "tpdeny", "tptoggle", "tphere", "tpo", "tpohere").forEach(CommandAPI::unregister);
    }

    private void createRequest(Player requester, String targetName, boolean isTpaHere) {
        String localServer = TeleportsNetwork.localServerNameIfKnown();
        if (localServer == null) {
            requester.sendMessage(Component.text("Teleport system not ready yet (server name unknown). Try again in a moment."));
            return;
        }

        Player localTarget = Bukkit.getPlayerExact(targetName);
        if (localTarget != null) {
            if (localTarget.getUniqueId().equals(requester.getUniqueId())) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.self"));
                return;
            }
            if (isToggled(localTarget)) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.toggled", localTarget.getName()));
                return;
            }

            TeleportRequestsRepository.create(
                requester.getUniqueId().toString(),
                requester.getName(),
                localServer,
                localTarget.getUniqueId().toString(),
                localServer,
                isTpaHere
            );

            if (isTpaHere) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpahere.sent", localTarget.getName()));
                localTarget.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpahere.received", requester.getName()));
            } else {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.sent", localTarget.getName()));
                localTarget.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.received", requester.getName()));
            }
            scheduleExpire(requester.getUniqueId().toString(), localTarget.getUniqueId().toString(), localTarget.getName());
            return;
        }

        lookupNetworkPlayer(targetName).thenAccept(info -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (info == null || info.uuid() == null || info.serverName() == null || info.serverName().isBlank()) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("general.error.player.invalid", targetName));
                return;
            }

            UUID targetId = info.uuid();
            String targetServer = info.serverName();
            String resolvedTargetName = (info.name() != null && !info.name().isBlank()) ? info.name() : targetName;

            if (targetId.equals(requester.getUniqueId())) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.self"));
                return;
            }

            PlayerEntity dbTarget = PlayerRepository.getPlayer(targetId);
            if (dbTarget != null && dbTarget.isTpToggle()) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.error.toggled", resolvedTargetName));
                return;
            }

            TeleportRequestsRepository.create(
                requester.getUniqueId().toString(),
                requester.getName(),
                localServer,
                targetId.toString(),
                targetServer,
                isTpaHere
            );

            if (isTpaHere) {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpahere.sent", resolvedTargetName));
                notifyOnServer("teleports.tpahere.received", targetServer, targetId, requester.getName());
            } else {
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.sent", resolvedTargetName));
                notifyOnServer("teleports.tpa.received", targetServer, targetId, requester.getName());
            }
            scheduleExpire(requester.getUniqueId().toString(), targetId.toString(), resolvedTargetName);
        }));
    }

    private CompletableFuture<NetworkPlayerInfo> lookupNetworkPlayer(String playerName) {
        if (playerName == null || playerName.isBlank()) return CompletableFuture.completedFuture(null);
        if (!Rapunzel.isBootstrapped()) return CompletableFuture.completedFuture(null);

        NetworkInfoService info = Rapunzel.context().services().get(NetworkInfoService.class);
        String needle = playerName.trim();
        return info.players()
            .thenApply(players -> {
                if (players == null) return null;
                for (NetworkPlayerInfo p : players) {
                    if (p == null || p.name() == null) continue;
                    if (p.name().equalsIgnoreCase(needle)) return p;
                }
                return null;
            })
            .exceptionally(ignored -> null);
    }

    private void queueTeleportToPlayer(UUID moverUuid, String targetServer, UUID targetPlayerUuid) {
        if (moverUuid == null || targetPlayerUuid == null) return;
        if (targetServer == null || targetServer.isBlank()) return;        

        String moverUuidString = moverUuid.toString();
        String arg = targetPlayerUuid.toString();
        CoreDatabase.runAsync(() -> PendingTeleportsRepository.create(
            moverUuidString,
            targetServer,
            TeleportsNetwork.TeleportsActions.TPA_TO_PLAYER,
            arg
        )).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                plugin.getLogger().warning("Failed to queue teleport for " + moverUuidString + ": " + error.getMessage());
                return;
            }
            connectPlayer(moverUuid, targetServer);
        }));
    }

    private void connectPlayer(UUID playerUuid, String targetServer) {
        if (playerUuid == null) return;
        if (targetServer == null || targetServer.isBlank()) return;
        if (TeleportsNetwork.isLocal(targetServer)) return;

        new NetworkEventBus(plugin.getMessenger()).sendToProxy(
            NetworkChannels.TELEPORTS_PROXY,
            new ProxyConnectRequest(playerUuid.toString(), targetServer)
        );
    }

    private void notifyOnServer(String messageKey, String serverName, UUID playerUuid, String... args) {
        if (messageKey == null || messageKey.isBlank()) return;
        if (playerUuid == null) return;

        if (TeleportsNetwork.isLocal(serverName)) {
            Player local = Bukkit.getPlayer(playerUuid);
            if (local != null) {
                local.sendMessage(plugin.getMessageHandler().getMessage(messageKey, args));
            }
            return;
        }

        if (serverName == null || serverName.isBlank()) return;
        new NetworkEventBus(plugin.getMessenger()).sendToServer(
            NetworkChannels.TELEPORTS_BACKEND,
            serverName,
            NotifyPlayerMessage.of(playerUuid.toString(), messageKey, args)
        );
    }

    private void acceptRequest(Player target, String requesterName) {
        List<TeleportRequest> requests = pruneExpired(TeleportRequestsRepository.findForTarget(target.getUniqueId().toString()));
        TeleportRequest req = selectRequest(target, requests, requesterName, false);

        if (req == null) return;

        TeleportRequestsRepository.delete(req.getId());

        UUID requesterId = UUID.fromString(req.getRequesterUuid());
        String requesterServer = req.getRequesterServer();
        String targetServer = req.getTargetServer();

        if (req.isTpaHere()) {
            // Move target to requester server, then teleport to requester.
            if (TeleportsNetwork.isLocal(requesterServer)) {
                Player requester = Bukkit.getPlayer(requesterId);
                if (requester != null) {
                    target.teleport(requester.getLocation());
                    target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted", req.getRequesterName()));
                    requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted_by", target.getName()));
                }
                return;
            }

            queueTeleportToPlayer(target.getUniqueId(), requesterServer, requesterId);
            target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted", req.getRequesterName()));
            notifyOnServer("teleports.tpaccept.accepted_by", requesterServer, requesterId, target.getName());
            return;
        }

        // Normal tpa: move requester to target server, then teleport requester to target.
        if (TeleportsNetwork.isLocal(targetServer)) {
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null) {
                requester.teleport(target.getLocation());
                target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted", req.getRequesterName()));
                requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted_by", target.getName()));
                return;
            }
        }

        queueTeleportToPlayer(requesterId, targetServer, target.getUniqueId());
        target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.accepted", req.getRequesterName()));
        notifyOnServer("teleports.tpaccept.accepted_by", requesterServer, requesterId, target.getName());
    }

    private void denyRequest(Player target, String requesterName) {
        List<TeleportRequest> requests = pruneExpired(TeleportRequestsRepository.findForTarget(target.getUniqueId().toString()));
        TeleportRequest req = selectRequest(target, requests, requesterName, true);

        if (req == null) return;

        TeleportRequestsRepository.delete(req.getId());

        UUID requesterId = UUID.fromString(req.getRequesterUuid());
        target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.denied_request", req.getRequesterName()));
        notifyOnServer("teleports.tpdeny.denied", req.getRequesterServer(), requesterId, target.getName());
    }

    private TeleportRequest selectRequest(Player target, List<TeleportRequest> requests, String requesterName, boolean deny) {
        if (requests.isEmpty()) {
            if (deny) target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.no_request"));
            else target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.no_request"));
            return null;
        }

        if (requesterName == null) {
            if (requests.size() == 1) return requests.getFirst();
            if (deny) target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.multiple_requests"));
            else target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.multiple_requests"));
            for (TeleportRequest r : requests) {
                if (deny) target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.list_entry", r.getRequesterName()));
                else target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.list_entry", r.getRequesterName()));
            }
            return null;
        }

        for (TeleportRequest r : requests) {
            if (r.getRequesterName() != null && r.getRequesterName().equalsIgnoreCase(requesterName)) return r;
        }

        if (deny) target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpdeny.error.no_request_from", requesterName));
        else target.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpaccept.error.no_request_from", requesterName));
        return null;
    }

    private List<TeleportRequest> pruneExpired(List<TeleportRequest> requests) {
        long cutoff = System.currentTimeMillis() - REQUEST_EXPIRE_MS;
        for (TeleportRequest req : requests) {
            if (req.getCreatedAt() < cutoff) TeleportRequestsRepository.delete(req.getId());
        }
        return requests.stream().filter(r -> r.getCreatedAt() >= cutoff).toList();
    }

    private void scheduleExpire(String requesterUuid, String targetUuid, String targetName) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            List<TeleportRequest> requests = TeleportRequestsRepository.findForTarget(targetUuid);
            long cutoff = System.currentTimeMillis() - REQUEST_EXPIRE_MS;
            for (TeleportRequest req : requests) {
                if (!req.getRequesterUuid().equalsIgnoreCase(requesterUuid)) continue;
                if (req.getCreatedAt() >= cutoff) continue;
                TeleportRequestsRepository.delete(req.getId());
                Player requester = Bukkit.getPlayer(UUID.fromString(requesterUuid));
                if (requester != null) {
                    requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.expired", targetName));
                }
            }
        }, 20L * 60);
    }

    private boolean isToggled(Player player) {
        PlayerEntity playerEntity = PlayerRepository.getPlayer(player.getUniqueId());
        return playerEntity != null && playerEntity.isTpToggle();
    }

    private void setToggled(Player player, boolean toggled) {
        PlayerEntity playerEntity = PlayerRepository.getPlayer(player.getUniqueId());
        if (playerEntity == null) return;
        playerEntity.setTpToggle(toggled);
        PlayerRepository.getInstance().save(playerEntity);
        flushAsync();
    }
}
