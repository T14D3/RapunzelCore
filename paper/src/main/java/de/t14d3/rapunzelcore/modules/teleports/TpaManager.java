package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;

public class TpaManager {
    private final RapunzelPaperCore plugin;
    private final List<TpaRequest> requests = new CopyOnWriteArrayList<>();
    private final int REQUEST_EXPIRE_TICKS = 20 * 60; // 60 seconds

    public TpaManager(RapunzelPaperCore plugin) {
        this.plugin = plugin;

        // Start central expiration checker - runs every 20 ticks (1 second)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpirations, 20L, 20L);
    }
    
    public void createRequest(Player requester, Player target, boolean isTpaHere) {
        // Remove existing request from same requester to same target
        requests.removeIf(req -> req.getRequester().equals(requester) && req.getTarget().equals(target));

        TpaRequest request = new TpaRequest(requester, target, isTpaHere);
        requests.add(request);
    }
    
    public List<TpaRequest> getRequests(Player target) {
        UUID targetId = target.getUniqueId();
        return requests.stream()
                .filter(req -> req.getTargetId().equals(targetId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public TpaRequest getRequest(Player target, String requesterName) {
        List<TpaRequest> targetRequests = getRequests(target);

        if (requesterName == null) {
            // If no name specified, return the first (only) request if there's only one
            return targetRequests.size() == 1 ? targetRequests.get(0) : null;
        }

        // Find request by requester name (case insensitive)
        return targetRequests.stream()
                .filter(req -> req.getRequester() != null &&
                        req.getRequester().getName().equalsIgnoreCase(requesterName))
                .findFirst()
                .orElse(null);
    }

    public TpaRequest removeRequest(Player target, String requesterName) {
        List<TpaRequest> targetRequests = getRequests(target);

        TpaRequest requestToRemove = null;
        if (requesterName == null) {
            // If no name specified, remove the first (only) request if there's only one
            if (targetRequests.size() == 1) {
                requestToRemove = targetRequests.get(0);
                requests.remove(requestToRemove);
            }
        } else {
            // Find and remove request by requester name
            requestToRemove = targetRequests.stream()
                    .filter(req -> req.getRequester() != null &&
                            req.getRequester().getName().equalsIgnoreCase(requesterName))
                    .findFirst()
                    .orElse(null);

            if (requestToRemove != null) {
                requests.remove(requestToRemove);
            }
        }

        return requestToRemove;
    }

    public boolean hasRequest(Player target) {
        return !getRequests(target).isEmpty();
    }

    public boolean isToggled(Player player) {
        de.t14d3.rapunzelcore.database.entities.Player dbPlayer = PlayerRepository.getPlayer(player.getUniqueId());
        return dbPlayer.isTpToggle();
    }

    public void setToggled(Player player, boolean toggled) {
        de.t14d3.rapunzelcore.database.entities.Player dbPlayer = PlayerRepository.getPlayer(player.getUniqueId());
        dbPlayer.setTpToggle(toggled);
        PlayerRepository.getInstance().save(dbPlayer);
        flushAsync();
    }

    private void checkExpirations() {
        // Check for expired requests every second
        int currentTick = Bukkit.getCurrentTick();
        Iterator<TpaRequest> iterator = requests.iterator();
        
        while (iterator.hasNext()) {
            TpaRequest req = iterator.next();
            Player requester = req.getRequester();
            Player target = req.getTarget();

            if (currentTick - req.getRequestTime() > REQUEST_EXPIRE_TICKS) {
                // Send expiration message to requester if they're still online
                if (requester != null && requester.isOnline()) {
                    requester.sendMessage(plugin.getMessageHandler().getMessage("teleports.tpa.expired", target.getName()));
                }
                iterator.remove();
                continue;
            }

            // Also remove if either player is offline
            if (requester == null || !requester.isOnline() || target == null || !target.isOnline()) {
                iterator.remove();
            }
        }
    }
}
