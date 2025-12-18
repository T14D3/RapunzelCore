package de.t14d3.rapunzelcore.modules.teleports;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TpaRequest {
    private final UUID requesterId;
    private final long requestTime;
    private final boolean isTpaHere;
    private BukkitTask expirationTask;

    public TpaRequest(Player requester, Player target, boolean isTpaHere) {
        this.requesterId = requester.getUniqueId();
        this.requestTime = System.currentTimeMillis();
        this.isTpaHere = isTpaHere;
    }

    public Player getRequester() {
        return Bukkit.getPlayer(requesterId);
    }

    public Player getTarget(UUID targetId) {
        return Bukkit.getPlayer(targetId);
    }

    public long getRequestTime() {
        return requestTime;
    }

    public boolean isTpaHere() {
        return isTpaHere;
    }

    public void setExpirationTask(BukkitTask task) {
        if (this.expirationTask != null) {
            this.expirationTask.cancel();
        }
        this.expirationTask = task;
    }

    public void cancelExpiration() {
        if (this.expirationTask != null) {
            this.expirationTask.cancel();
            this.expirationTask = null;
        }
    }

    public void teleport(UUID targetId) {
        Player requester = getRequester();
        Player target = getTarget(targetId);

        if (requester == null || target == null) return;

        if (isTpaHere) {
            // tpahere: teleport target to requester
            target.teleport(requester.getLocation());
        } else {
            // tpa: teleport requester to target
            requester.teleport(target.getLocation());
        }
    }
}
