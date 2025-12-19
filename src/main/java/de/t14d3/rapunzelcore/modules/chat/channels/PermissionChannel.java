package de.t14d3.rapunzelcore.modules.chat.channels;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Permission-based channel that sends messages to players who have the required permission.
 */
public class PermissionChannel extends AbstractChannel {

    public PermissionChannel(String name, String format, String shortcut, String permission, boolean crossServer) {
        super(name, format, shortcut, permission, crossServer);
    }

    @Override
    public List<Player> getReceivers(Player sender) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission(permission != null ? permission : ""))
                .collect(Collectors.toList());
    }
}
