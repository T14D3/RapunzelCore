package de.t14d3.rapunzelcore.modules.chat.channels;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface for chat channels. Each channel can define its own message formatting,
 * receiver list, and permissions.
 */
public interface Channel {

    /**
     * Gets the unique name of this channel.
     * @return The channel name
     */
    String getName();

    /**
     * Gets the format string for this channel.
     * @return The format string
     */
    String getFormat();

    /**
     * Gets the shortcut for this channel.
     * @return The shortcut
     */
    String getShortcut();

    /**
     * Gets the permission required to use this channel.
     * @return The permission string, or null if none required
     */
    String getPermission();

    /**
     * Checks if this channel supports cross-server communication.
     * @return True if cross-server enabled
     */
    boolean isCrossServer();


    void sendMessage(Player sender, Component message);

    /**
     * Gets the list of players who should receive this message.
     * @param sender The player sending the message
     * @return List of receivers
     */
    List<Player> getReceivers(Player sender);

    /**
     * Checks if the player has permission to use this channel.
     * @param sender The player to check
     * @return True if the player can use this channel
     */
    boolean hasPermission(Player sender);
}
