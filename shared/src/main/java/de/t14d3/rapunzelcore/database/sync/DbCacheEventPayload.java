package de.t14d3.rapunzelcore.database.sync;

import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.cache.CacheKey;

public record DbCacheEventPayload(
    String operation,
    String entityClassName,
    String id
) {
    public static DbCacheEventPayload from(CacheEvent event) {
        if (event == null || event.key() == null || event.operation() == null) {
            return null;
        }
        CacheKey key = event.key();
        return new DbCacheEventPayload(event.operation().name(), key.entityClassName(), key.id());
    }

    public CacheEvent toCacheEvent() {
        CacheEvent.Operation op = CacheEvent.Operation.valueOf(operation);
        return new CacheEvent(op, new CacheKey(entityClassName, id));
    }
}

