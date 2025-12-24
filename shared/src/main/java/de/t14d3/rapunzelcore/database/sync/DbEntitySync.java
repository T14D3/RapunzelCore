package de.t14d3.rapunzelcore.database.sync;

import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.queue.NetworkOutboxMessage;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.cache.CacheEventSink;
import de.t14d3.spool.cache.CacheProvider;
import de.t14d3.spool.cache.LocalMemoryCacheProvider;
import de.t14d3.spool.core.EntityManager;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DbEntitySync implements AutoCloseable {
    public interface Listener {
        void onCacheEvent(CacheEvent event, String sourceServer);
    }

    private final Messenger messenger;
    private final NetworkEventBus bus;
    private final NetworkEventBus.Subscription subscription;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final CacheProvider cacheProvider;
    private final EntityManager entityManager;

    public DbEntitySync(SpoolDatabase database, Messenger messenger) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        Objects.requireNonNull(database, "database");

        this.bus = new NetworkEventBus(this.messenger);
        this.cacheProvider = new LocalMemoryCacheProvider();
        this.entityManager = database.entityManager()
            .withCacheProvider(cacheProvider)
            .withCacheTtl(Duration.ofMinutes(10))
            .withCacheEventSink(new BroadcastingCacheEventSink());

        this.subscription = bus.register(
            NetworkChannels.DB_CACHE_EVENT,
            DbCacheEventPayload.class,
            (payload, sourceServer) -> {
                if (payload == null) return;
                String localServerName = this.messenger.getServerName();
                if (sourceServer != null && localServerName != null) {
                    String local = localServerName.trim();
                    if (!local.isBlank()
                        && !"unknown".equalsIgnoreCase(local)
                        && sourceServer.equalsIgnoreCase(local)) {
                        return;
                    }
                }

                CacheEvent event = payload.toCacheEvent();
                if (event == null || event.key() == null) return;
                cacheProvider.invalidate(event.key());

                dispatch(event, sourceServer);
            }
        );
    }

    public void register(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void dispatch(CacheEvent event, String sourceServer) {
        if (event == null) return;
        for (Listener listener : listeners) {
            try {
                listener.onCacheEvent(event, sourceServer);
            } catch (Exception ignored) {
            }
        }
    }

    private final class BroadcastingCacheEventSink implements CacheEventSink {  
        @Override
        public void append(CacheEvent event) {
            if (event == null) return;
            if (event.key() != null
                && NetworkOutboxMessage.class.getName().equals(event.key().entityClassName())) {
                // Avoid infinite recursion: broadcasting a cache event enqueues a network outbox row, which itself emits a cache event.
                return;
            }

            // Broadcast to other servers.
            DbCacheEventPayload payload = DbCacheEventPayload.from(event);      
            if (payload != null) {
                bus.sendToAll(NetworkChannels.DB_CACHE_EVENT, payload);
            }
        }
    }

    @Override
    public void close() {
        subscription.close();
        try {
            cacheProvider.close();
        } catch (Exception ignored) {
        }
    }
}
