package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.ChannelRepository;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.rapunzellib.config.YamlConfig;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private DbEntitySync.Listener syncListener;
    private final AtomicBoolean reloadQueued = new AtomicBoolean(false);

    public ChannelManager(YamlConfig config) {
        this.channelRepository = ChannelRepository.getInstance();
        this.playerRepository = PlayerRepository.getInstance();
        syncChannelsFromConfig(config);
        reloadChannels();
        registerSyncListenerIfAvailable();
    }

    private void registerSyncListenerIfAvailable() {
        if (syncListener != null) return;
        DbEntitySync sync = CoreDatabase.entitySync();
        if (sync == null) return;
        syncListener = this::onCacheEvent;
        sync.register(syncListener);
    }

    public void close() {
        DbEntitySync.Listener listener = syncListener;
        syncListener = null;
        DbEntitySync sync = CoreDatabase.entitySync();
        if (sync != null && listener != null) {
            sync.unregister(listener);
        }
    }

    private void onCacheEvent(CacheEvent event, String sourceServer) {    
        if (event == null || event.key() == null) return;
        if (!Channel.class.getName().equals(event.key().entityClassName())) return;
        if (!reloadQueued.compareAndSet(false, true)) return;
        CoreDatabase.runAsync(() -> {
            try {
                reloadChannels();
            } finally {
                reloadQueued.set(false);
            }
        });
    }

    /**
     * Loads channels from configuration file into the database.
     */
    private void syncChannelsFromConfig(YamlConfig config) {
        if (config == null) return;
        Set<String> channelNames = childKeys(config, "channels");
        if (channelNames.isEmpty()) return;
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

    private static Set<String> childKeys(YamlConfig config, String root) {
        if (config == null || root == null || root.isBlank()) return Set.of();
        String prefix = root.endsWith(".") ? root : (root + ".");
        Set<String> out = new LinkedHashSet<>();
        for (String key : config.keys(true)) {
            if (key == null || !key.startsWith(prefix)) continue;
            String rest = key.substring(prefix.length());
            int dot = rest.indexOf('.');
            String child = dot >= 0 ? rest.substring(0, dot) : rest;
            if (!child.isBlank()) out.add(child);
        }
        return out;
    }

    /**
     * Gets a channel by name.
     * @param name The channel name
     * @return The channel entity, or null if not found
     */
    public Channel getChannel(String name) {
        if (name == null || name.isBlank()) return null;
        return channels.get(name.toLowerCase());
    }

    public Map<String, Channel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    /**
     * Gets all channels that a player has permission to use.
     * @param playerEntity The player
     * @return Map of channel names to channel entities
     */
    public Map<String, Channel> getAllowedChannels(PlayerEntity playerEntity) {
        return channels.values().stream()
            .filter(channel -> channel.hasPermission(playerEntity))
            .collect(Collectors.toMap(Channel::getName, ch -> ch));
    }

    /**
     * Gets the default channel.
     * @return The default channel entity
     */
    public Channel getDefaultChannel() {
        return defaultChannel;
    }

    public Channel getMainChannel(PlayerEntity playerEntity) {
        if (playerEntity == null) return defaultChannel;
        String main = playerEntity.getMainChannel();
        if (main == null || main.isBlank()) {
            return defaultChannel;
        }
        Channel channel = getChannel(main);
        return channel != null ? channel : defaultChannel;
    }

    public boolean setMainChannel(PlayerEntity playerEntity, Channel channel) {
        if (playerEntity == null || channel == null) return false;
        if (!channel.hasPermission(playerEntity)) return false;
        if (!isJoined(playerEntity, channel)) {
            joinChannel(playerEntity, channel);
        }
        playerEntity.setMainChannel(channel.getName());
        runLocked(() -> playerRepository.save(playerEntity));
        flushAsync();
        return true;
    }

    public boolean joinChannel(PlayerEntity playerEntity, Channel channel) {
        if (playerEntity == null || channel == null) return false;
        if (!channel.hasPermission(playerEntity)) return false;

        // Implicit membership channels don't persist membership.
        if (channel.getType() != Channel.ChannelType.PLAYER) {
            if (playerEntity.getMainChannel() == null || playerEntity.getMainChannel().isBlank()) {
                playerEntity.setMainChannel(channel.getName());
                runLocked(() -> playerRepository.save(playerEntity));
                flushAsync();
            }
            return true;
        }

        boolean changed = playerEntity.getJoinedChannels().add(channel);
        if (changed) {
            if (playerEntity.getMainChannel() == null || playerEntity.getMainChannel().isBlank()) {
                playerEntity.setMainChannel(channel.getName());
            }
            runLocked(() -> playerRepository.save(playerEntity));
            flushAsync();
        }
        return true;
    }

    public boolean leaveChannel(PlayerEntity playerEntity, Channel channel) {
        if (playerEntity == null || channel == null) return false;

        // Implicit membership channels can't be left (they are rule-based).
        if (channel.getType() != Channel.ChannelType.PLAYER) {
            return false;
        }

        boolean changed = playerEntity.getJoinedChannels().remove(channel);
        if (!changed) return false;

        if (channel.getName().equalsIgnoreCase(playerEntity.getMainChannel())) {
            playerEntity.setMainChannel(defaultChannel != null ? defaultChannel.getName() : "");
        }

        runLocked(() -> playerRepository.save(playerEntity));
        flushAsync();
        return true;
    }

    public boolean isJoined(PlayerEntity playerEntity, Channel channel) {
        if (playerEntity == null || channel == null) return false;
        if (!channel.hasPermission(playerEntity)) return false;

        return switch (channel.getType()) {
            case GLOBAL, LOCAL, PERMISSION -> true;
            case PLAYER -> playerEntity.getJoinedChannels().contains(channel);
        };
    }

    public Set<Channel> getJoinedChannels(PlayerEntity playerEntity) {
        if (playerEntity == null) return Set.of();

        // Implicit membership channels: any channel with permission is effectively joined.
        Set<Channel> implicit = channels.values().stream()
            .filter(ch -> ch.getType() != Channel.ChannelType.PLAYER)
            .filter(ch -> ch.hasPermission(playerEntity))
            .collect(Collectors.toSet());

        // Explicit membership channels: persisted ManyToMany membership.
        Set<Channel> explicit = playerEntity.getJoinedChannels().stream()
            .filter(ch -> ch != null && ch.getType() == Channel.ChannelType.PLAYER)
            .filter(ch -> ch.hasPermission(playerEntity))
            .collect(Collectors.toSet());

        implicit.addAll(explicit);
        return implicit;
    }

    /**
     * Returns the persisted members of a PLAYER channel.
     * <p>
     * For other channel types membership is implicit and should be computed by the platform at runtime.
     */
    public Set<PlayerEntity> getPersistedMembers(Channel channel) {
        if (channel == null) return Set.of();
        if (channel.getType() != Channel.ChannelType.PLAYER) return Set.of();
        return channel.getMembers();
    }

    /**
     * Reloads all channels from the database.
     */
    public void reloadChannels() {
        Map<String, Channel> fresh = locked(() -> {
            var list = channelRepository.findAll();
            for (Channel ch : list) {
                if (ch != null) {
                    CoreDatabase.getEntityManager().refresh(ch);
                }
            }
            return list.stream().collect(Collectors.toMap(
                ch -> ch.getName().toLowerCase(),
                ch -> ch,
                (a, b) -> a,
                LinkedHashMap::new
            ));
        });
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
