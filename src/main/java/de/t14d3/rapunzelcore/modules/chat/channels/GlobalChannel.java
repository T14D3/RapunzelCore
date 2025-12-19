package de.t14d3.rapunzelcore.modules.chat.channels;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Global channel that sends messages to all online players.
 */
public class GlobalChannel extends AbstractChannel {

    public GlobalChannel(String name, String format, String shortcut, String permission, boolean crossServer) {
        super(name, format, shortcut, permission, crossServer);
    }

    @Override
    public List<Player> getReceivers(Player sender) {
        return List.copyOf(Bukkit.getOnlinePlayers());
    }
}
