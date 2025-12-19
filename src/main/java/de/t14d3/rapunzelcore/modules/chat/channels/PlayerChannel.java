package de.t14d3.rapunzelcore.modules.chat.channels;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player-managed channel that holds a list of players that can access it.
 * Not persisted to config.
 */
public class PlayerChannel extends AbstractChannel {
    private final List<UUID> allowedPlayers;

    public PlayerChannel(String name, String format, String shortcut, String permission, boolean crossServer) {
        super(name, format, shortcut, permission, crossServer);
        this.allowedPlayers = new ArrayList<>();
    }

    /**
     * Adds a player to the allowed list.
     * @param player The player to add
     */
    public void addPlayer(Player player) {
        if (!allowedPlayers.contains(player.getUniqueId())) {
            allowedPlayers.add(player.getUniqueId());
        }
    }

    /**
     * Removes a player from the allowed list.
     * @param player The player to remove
     */
    public void removePlayer(Player player) {
        allowedPlayers.remove(player.getUniqueId());
    }


    @Override
    public List<Player> getReceivers(Player sender) {
        return allowedPlayers.stream()
                .map(uuid -> sender.getServer().getPlayer(uuid))
                .filter(player -> player != null && player.isOnline())
                .toList();
    }

    @Override
    public boolean hasPermission(Player sender) {
        return sender.hasPermission("rapunzelcore.chat.channels.bypass") || allowedPlayers.contains(sender.getUniqueId());
    }
}
