package de.t14d3.rapunzelcore.modules.teleports;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TpaRequest {
    private final Player requester;
    private final Player target;
    private final int requestTime;
    private final boolean isTpaHere;

    public TpaRequest(Player requester, Player target, boolean isTpaHere) {
        this.requester = requester;
        this.target = target;
        this.requestTime = Bukkit.getCurrentTick();
        this.isTpaHere = isTpaHere;
    }

    public Player getRequester() {
        return requester;
    }

    public Player getTarget() {
        return target;
    }

    public UUID getTargetId() {
        return target.getUniqueId();
    }

    public int getRequestTime() {
        return requestTime;
    }

    public void teleport() {
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
