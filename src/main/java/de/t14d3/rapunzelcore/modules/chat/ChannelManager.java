package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.chat.channels.*;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages chat channels and player channel assignments.
 */
public class ChannelManager {
    private final Map<String, Channel> channels = new HashMap<>();
    private Channel defaultChannel;
    private static final NamespacedKey JOINED_CHANNELS_KEY = new NamespacedKey(Main.getInstance(), "joined_channels");
    private static final NamespacedKey MAIN_CHANNEL_KEY = new NamespacedKey(Main.getInstance(), "main_channel");

    public ChannelManager(FileConfiguration config) {

        if (!config.contains("chat.channels")) {
            return;
        }

        channels.clear();
        defaultChannel = null;

        for (String channelName : config.getConfigurationSection("chat.channels").getKeys(false)) {
            String path = "chat.channels." + channelName;
            String type = config.getString(path + ".type", "global").toLowerCase();
            String permission = config.getString(path + ".permission", "");
            String format = config.getString(path + ".format", "");
            String shortcut = config.getString(path + ".shortcut", "");
            boolean crossServer = config.getBoolean(path + ".crossServer", false);
            double range = config.getDouble(path + ".range", 200.0);

            Channel channel = createChannel(channelName, type, format, shortcut, permission, crossServer, range);
            if (channel != null) {
                registerChannel(channel);
            }
        }
    }

    private Channel createChannel(String name, String type, String format, String shortcut, String permission, boolean crossServer, double range) {
        return switch (type) {
            case "global" -> new GlobalChannel(name, format, shortcut, permission, crossServer);
            case "local" -> new LocalChannel(name, format, shortcut, permission, crossServer, range);
            case "permission" -> new PermissionChannel(name, format, shortcut, permission, crossServer);
            case "player" -> new PlayerChannel(name, format, shortcut, permission, crossServer);
            default -> null;
        };
    }


    /**
     * Registers a channel.
     * @param channel The channel to register
     */
    public void registerChannel(Channel channel) {
        channels.put(channel.getName(), channel);
        // Set default channel - first global channel or explicitly marked as default
        if (defaultChannel == null && channel instanceof GlobalChannel) {
            defaultChannel = channel;
        }
    }

    /**
     * Gets a channel by name.
     * @param name The channel name
     * @return The channel, or null if not found
     */
    public Channel getChannel(String name) {
        return channels.get(name);
    }

    /**
     * Gets all registered channels.
     * @return Map of channel names to channels
     */
    public Map<String, Channel> getChannels() {
        return new HashMap<>(channels);
    }

    public Map<String, Channel> getChannels(Player player) {
        Map<String, Channel> channels = new HashMap<>();
        for (Channel channel : this.getChannels().values()) {
            if (channel.hasPermission(player)) {
                channels.put(channel.getName(), channel);
            }
        }
        return channels;
    }

    /**
     * Gets the joined channels for a player.
     * @param player The player
     * @return Set of joined channels
     */
    public Set<Channel> getJoinedChannels(Player player) {
        String joinedStr = player.getPersistentDataContainer().get(JOINED_CHANNELS_KEY, PersistentDataType.STRING);
        if (joinedStr == null || joinedStr.isEmpty()) {
            // Initialize with default channel
            Set<Channel> joined = new HashSet<>();
            joined.add(defaultChannel);
            setJoinedChannels(player, joined);
            return joined;
        }
        return Arrays.stream(joinedStr.split(","))
                .map(channels::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Sets the joined channels for a player.
     * @param player The player
     * @param channels The set of channels
     */
    private void setJoinedChannels(Player player, Set<Channel> channels) {
        String joinedStr = channels.stream()
                .map(Channel::getName)
                .collect(Collectors.joining(","));
        player.getPersistentDataContainer().set(JOINED_CHANNELS_KEY, PersistentDataType.STRING, joinedStr);
    }

    /**
     * Gets the main channel for a player.
     * @param player The player
     * @return The main channel, or the default channel if none set
     */
    public Channel getMainChannel(Player player) {
        String channelName = player.getPersistentDataContainer().get(MAIN_CHANNEL_KEY, PersistentDataType.STRING);
        if (channelName != null) {
            Channel channel = channels.get(channelName);
            if (channel != null && getJoinedChannels(player).contains(channel)) {
                return channel;
            }
        }
        return defaultChannel;
    }

    /**
     * Sets the main channel for a player.
     * @param player The player
     * @param channel The channel to set
     * @return True if successful, false if player is not joined to the channel
     */
    public boolean setMainChannel(Player player, Channel channel) {
        if (!getJoinedChannels(player).contains(channel)) {
            return false;
        }
        player.getPersistentDataContainer().set(MAIN_CHANNEL_KEY, PersistentDataType.STRING, channel.getName());
        return true;
    }

    /**
     * Joins a player to a channel.
     * @param player The player
     * @param channel The channel to join
     * @return True if successful, false if no permission or already joined
     */
    public boolean joinChannel(Player player, Channel channel) {
        if (!channel.hasPermission(player)) {
            return false;
        }
        Set<Channel> joined = getJoinedChannels(player);
        if (joined.contains(channel)) {
            return false; // Already joined
        }
        joined.add(channel);
        setJoinedChannels(player, joined);
        return true;
    }

    /**
     * Leaves a player from a channel.
     * @param player The player
     * @param channel The channel to leave
     * @return True if successful, false if not joined or trying to leave default
     */
    public boolean leaveChannel(Player player, Channel channel) {
        if (channel == defaultChannel) {
            return false; // Cannot leave default channel
        }
        Set<Channel> joined = getJoinedChannels(player);
        if (!joined.contains(channel)) {
            return false; // Not joined
        }
        joined.remove(channel);
        setJoinedChannels(player, joined);
        // If leaving main channel, set to default
        if (getMainChannel(player) == channel) {
            setMainChannel(player, defaultChannel);
        }
        return true;
    }


    /**
     * Gets the default channel.
     * @return The default channel
     */
    public Channel getDefaultChannel() {
        return defaultChannel;
    }
}
