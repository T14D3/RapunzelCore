package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.ChannelRepository;
import de.t14d3.rapunzelcore.database.entities.Player;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.t14d3.rapunzelcore.database.CoreDatabase.flushAsync;
import static de.t14d3.rapunzelcore.database.CoreDatabase.locked;
import static de.t14d3.rapunzelcore.database.CoreDatabase.runLocked;

/**
 * Manages chat channels and player channel assignments.
 * Uses Spool-backed entities for persistence.
 */
public class ChannelManager {
    private final ChannelRepository channelRepository;
    private final PlayerRepository playerRepository;
    private volatile Map<String, Channel> channels = new LinkedHashMap<>();
    private volatile Channel defaultChannel;

    public ChannelManager(FileConfiguration config) {
        this.channelRepository = ChannelRepository.getInstance();
        this.playerRepository = PlayerRepository.getInstance();
        syncChannelsFromConfig(config);
        reloadChannels();
    }

    /**
     * Loads channels from configuration file into the database.
     */
    private void syncChannelsFromConfig(FileConfiguration config) {
        if (config == null || !config.contains("channels")) {
            return;
        }
        Set<String> channelNames = config.getConfigurationSection("channels").getKeys(false);
        runLocked(() -> {
            boolean anyDefault = false;
            for (String channelName : channelNames) {
                Channel desired = Channel.fromConfig(channelName, config);
                Channel existing = channelRepository.findOneBy(Map.of("name", desired.getName()));
                if (existing == null) {
                    channelRepository.save(desired);
                } else {
                    existing.setType(desired.getType());
                    existing.setFormat(desired.getFormat());
                    existing.setShortcut(desired.getShortcut());
                    existing.setPermission(desired.getPermission());
                    existing.setCrossServer(desired.isCrossServer());
                    existing.setRange(desired.getRange());
                    existing.setDefault(desired.isDefault());
                    channelRepository.save(existing);
                }
                anyDefault |= desired.isDefault();
            }

            if (anyDefault) {
                Channel configuredDefault = channelRepository.findAll().stream()
                    .filter(Channel::isDefault)
                    .findFirst()
                    .orElse(null);
                if (configuredDefault != null) {
                    for (Channel channel : channelRepository.findAll()) {
                        if (!channel.getName().equalsIgnoreCase(configuredDefault.getName()) && channel.isDefault()) {
                            channel.setDefault(false);
                            channelRepository.save(channel);
                        }
                    }
                }
            }

            de.t14d3.rapunzelcore.database.CoreDatabase.getEntityManager().flush();
        });
    }

    /**
     * Gets a channel by name.
     * @param name The channel name
     * @return The channel entity, or null if not found
     */
    public Channel getChannel(String name) {
        if (name == null || name.isBlank()) return null;
        String key = name.toLowerCase();
        Channel cached = channels.get(key);
        if (cached != null) return cached;
        return locked(() -> channelRepository.findOneBy(Map.of("name", name)));
    }

    public Map<String, Channel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    /**
     * Gets all channels that a player has permission to use.
     * @param player The player
     * @return Map of channel names to channel entities
     */
    public Map<String, Channel> getAllowedChannels(Player player) {
        return channels.values().stream()
            .filter(channel -> channel.hasPermission(player))
            .collect(Collectors.toMap(Channel::getName, ch -> ch));
    }

    /**
     * Gets the default channel.
     * @return The default channel entity
     */
    public Channel getDefaultChannel() {
        return defaultChannel;
    }

    public Channel getMainChannel(Player player) {
        if (player == null) return defaultChannel;
        String main = player.getMainChannel();
        if (main == null || main.isBlank()) {
            return defaultChannel;
        }
        Channel channel = getChannel(main);
        return channel != null ? channel : defaultChannel;
    }

    public boolean setMainChannel(Player player, Channel channel) {
        if (player == null || channel == null) return false;
        if (!channel.hasPermission(player)) return false;
        if (!isJoined(player, channel)) {
            joinChannel(player, channel);
        }
        player.setMainChannel(channel.getName());
        runLocked(() -> playerRepository.save(player));
        flushAsync();
        return true;
    }

    public boolean joinChannel(Player player, Channel channel) {
        if (player == null || channel == null) return false;
        if (!channel.hasPermission(player)) return false;

        // Implicit membership channels don't persist membership.
        if (channel.getType() != Channel.ChannelType.PLAYER) {
            if (player.getMainChannel() == null || player.getMainChannel().isBlank()) {
                player.setMainChannel(channel.getName());
                runLocked(() -> playerRepository.save(player));
                flushAsync();
            }
            return true;
        }

        boolean changed = player.getJoinedChannels().add(channel);
        if (changed) {
            if (player.getMainChannel() == null || player.getMainChannel().isBlank()) {
                player.setMainChannel(channel.getName());
            }
            runLocked(() -> playerRepository.save(player));
            flushAsync();
        }
        return true;
    }

    public boolean leaveChannel(Player player, Channel channel) {
        if (player == null || channel == null) return false;

        // Implicit membership channels can't be left (they are rule-based).
        if (channel.getType() != Channel.ChannelType.PLAYER) {
            return false;
        }

        boolean changed = player.getJoinedChannels().remove(channel);
        if (!changed) return false;

        if (channel.getName().equalsIgnoreCase(player.getMainChannel())) {
            player.setMainChannel(defaultChannel != null ? defaultChannel.getName() : "");
        }

        runLocked(() -> playerRepository.save(player));
        flushAsync();
        return true;
    }

    public boolean isJoined(Player player, Channel channel) {
        if (player == null || channel == null) return false;
        if (!channel.hasPermission(player)) return false;

        return switch (channel.getType()) {
            case GLOBAL, LOCAL, PERMISSION -> true;
            case PLAYER -> player.getJoinedChannels().contains(channel);
        };
    }

    public Set<Channel> getJoinedChannels(Player player) {
        if (player == null) return Set.of();

        // Implicit membership channels: any channel with permission is effectively joined.
        Set<Channel> implicit = channels.values().stream()
            .filter(ch -> ch.getType() != Channel.ChannelType.PLAYER)
            .filter(ch -> ch.hasPermission(player))
            .collect(Collectors.toSet());

        // Explicit membership channels: persisted ManyToMany membership.
        Set<Channel> explicit = player.getJoinedChannels().stream()
            .filter(ch -> ch != null && ch.getType() == Channel.ChannelType.PLAYER)
            .filter(ch -> ch.hasPermission(player))
            .collect(Collectors.toSet());

        implicit.addAll(explicit);
        return implicit;
    }

    /**
     * Returns the persisted members of a PLAYER channel.
     * <p>
     * For other channel types membership is implicit and should be computed by the platform at runtime.
     */
    public Set<Player> getPersistedMembers(Channel channel) {
        if (channel == null) return Set.of();
        if (channel.getType() != Channel.ChannelType.PLAYER) return Set.of();
        return channel.getMembers();
    }

    /**
     * Reloads all channels from the database.
     */
    public void reloadChannels() {
        Map<String, Channel> fresh = locked(() -> channelRepository.findAll().stream()
            .collect(Collectors.toMap(ch -> ch.getName().toLowerCase(), ch -> ch, (a, b) -> a, LinkedHashMap::new)));
        channels = fresh;

        defaultChannel = fresh.values().stream()
            .filter(Channel::isDefault)
            .findFirst()
            .orElseGet(() -> fresh.values().stream()
                .filter(ch -> ch.getType() == Channel.ChannelType.GLOBAL)
                .findFirst()
                .orElse(fresh.values().stream().findFirst().orElse(null)));
    }
}
