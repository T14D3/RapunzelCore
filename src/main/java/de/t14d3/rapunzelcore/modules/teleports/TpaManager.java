package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.entities.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {
    private final Main plugin;
    private final Map<UUID, TpaRequest> requests = new ConcurrentHashMap<>();
    private final long REQUEST_EXPIRE_TIME = 300 * 1000; // 5 minutes in milliseconds
    
    public TpaManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public void createRequest(Player requester, Player target, boolean isTpaHere) {
        // Cancel any existing request from this requester to this target
        requests.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            TpaRequest req = entry.getValue();
            if (req.getRequester() != null &&
                req.getRequester().getUniqueId().equals(requester.getUniqueId()) &&
                targetId.equals(target.getUniqueId())) {
                req.cancelExpiration();
                return true;
            }
            return false;
        });
        
        TpaRequest request = new TpaRequest(requester, target, isTpaHere);
        requests.put(target.getUniqueId(), request);
        
        // Set expiration task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TpaRequest req = requests.remove(target.getUniqueId());
            if (req != null) {
                Player r = req.getRequester();
                if (r != null && r.isOnline()) {
                    r.sendMessage(plugin.getMessage("teleports.tpa.expired", target.getName()));
                }
                req.cancelExpiration();
            }
        }, REQUEST_EXPIRE_TIME / 50); // Convert to ticks
        
        request.setExpirationTask(task);
    }
    
    public TpaRequest getRequest(Player target) {
        return requests.get(target.getUniqueId());
    }
    
    public TpaRequest removeRequest(Player target) {
        TpaRequest request = requests.remove(target.getUniqueId());
        if (request != null) {
            request.cancelExpiration();
        }
        return request;
    }
    
    public boolean hasRequest(Player target) {
        return requests.containsKey(target.getUniqueId());
    }
    
    public boolean isToggled(Player player) {
        de.t14d3.rapunzelcore.entities.Player dbPlayer = PlayerRepository.getPlayer(player);
        return dbPlayer.isTpToggle();
    }

    public void setToggled(Player player, boolean toggled) {
        de.t14d3.rapunzelcore.entities.Player dbPlayer = PlayerRepository.getPlayer(player);
        dbPlayer.setTpToggle(toggled);
        PlayerRepository.getInstance().save(dbPlayer);
        CoreDatabase.getEntityManager().flush();
    }
    
    public void cleanup() {
        // Clean up expired requests
        long currentTime = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> {
            UUID targetId = entry.getKey();
            TpaRequest req = entry.getValue();
            Player target = req.getTarget(targetId);
            if (currentTime - req.getRequestTime() > REQUEST_EXPIRE_TIME ||
                req.getRequester() == null ||
                !req.getRequester().isOnline() ||
                target == null ||
                !target.isOnline()) {
                req.cancelExpiration();
                return true;
            }
            return false;
        });
    }
}
