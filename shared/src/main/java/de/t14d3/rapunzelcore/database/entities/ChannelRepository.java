package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.sync.DbEntitySync;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.repository.EntityRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChannelRepository extends EntityRepository<Channel> {
    public interface ChangeListener {
        void onChannelChanged();
    }

    private static final ChannelRepository instance = new ChannelRepository();
    private final Map<String, Channel> channelsByName = new ConcurrentHashMap<>();
    private final List<ChangeListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean syncRegistered;

    private ChannelRepository() {
        super(CoreDatabase.getEntityManager(), Channel.class);
        registerSyncListenerIfAvailable();
    }

    public static ChannelRepository getInstance() {
        return instance;
    }

    private void registerSyncListenerIfAvailable() {
        if (syncRegistered) return;
        DbEntitySync sync = CoreDatabase.entitySync();
        if (sync == null) return;
        synchronized (this) {
            if (syncRegistered) return;
            sync.register(this::onCacheEvent);
            syncRegistered = true;
        }
    }

    private void onCacheEvent(CacheEvent event, String sourceServer) {
        if (event == null || event.key() == null) return;
        if (!Channel.class.getName().equals(event.key().entityClassName())) return;
        CacheEvent.Operation operation = event.operation();
        String id = event.key().id();
        if (operation == null || id == null) return;

        String key = normalize(id);
        CoreDatabase.runLockedAsync(() -> {
            Channel cached = channelsByName.get(key);
            if (cached != null) {
                if (operation == CacheEvent.Operation.DELETE) {
                    channelsByName.remove(key, cached);
                    CoreDatabase.getEntityManager().detach(cached);
                } else {
                    CoreDatabase.getEntityManager().refresh(cached);
                }
            }
            notifyListeners();
        });
    }

    private static String normalize(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private void cache(Channel entity) {
        if (entity == null || entity.getName() == null) return;
        channelsByName.put(normalize(entity.getName()), entity);
    }

    private void removeFromCache(String name) {
        String key = normalize(name);
        if (key == null) return;
        Channel cached = channelsByName.get(key);
        if (cached != null) {
            channelsByName.remove(key, cached);
        }
    }

    private void notifyListeners() {
        for (ChangeListener listener : listeners) {
            try {
                listener.onChannelChanged();
            } catch (Exception ignored) {
            }
        }
    }

    public void registerChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregisterChangeListener(ChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public Map<String, Channel> snapshot() {
        return Map.copyOf(channelsByName);
    }

    public void replaceCache(Map<String, Channel> latest) {
        channelsByName.clear();
        if (latest == null || latest.isEmpty()) return;
        for (Channel channel : latest.values()) {
            cache(channel);
        }
    }

    public static Channel getChannel(String name) {
        if (name == null || name.isBlank()) return null;
        instance.registerSyncListenerIfAvailable();
        String key = normalize(name);
        Channel cached = instance.channelsByName.get(key);
        if (cached != null) return cached;

        return CoreDatabase.locked(() -> instance.channelsByName.computeIfAbsent(
            key,
            k -> instance.findById(name.trim())
        ));
    }

    @Override
    public Channel save(Channel entity) {
        if (entity == null) return null;
        registerSyncListenerIfAvailable();
        super.save(entity);
        cache(entity);
        notifyListeners();
        return entity;
    }

    public Channel save(Channel entity, boolean flush) {
        if (entity == null) return null;
        registerSyncListenerIfAvailable();
        if (flush) {
            CoreDatabase.locked(() -> {
                super.save(entity);
                CoreDatabase.getEntityManager().flush();
                return entity;
            });
        } else {
            super.save(entity);
        }
        cache(entity);
        notifyListeners();
        return entity;
    }

    @Override
    public void delete(Channel entity) {
        if (entity == null) return;
        registerSyncListenerIfAvailable();
        super.delete(entity);
        removeFromCache(entity.getName());
        notifyListeners();
    }
}
