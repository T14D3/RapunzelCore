package de.t14d3.rapunzelcore.modules.chat.channels;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Local channel that sends messages to nearby players within a range.
 */
public class LocalChannel extends AbstractChannel {
    private final double range;

    public LocalChannel(String name, String format, String shortcut, String permission, boolean crossServer, double range) {
        super(name, format, shortcut, permission, crossServer);
        this.range = range;
    }

    @Override
    public List<Player> getReceivers(Player sender) {
        return (List<Player>) sender.getLocation().getNearbyPlayers(range);
    }
}
