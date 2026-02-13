package de.t14d3.rapunzelcore.network;

/**
 * Defines the priority order for network transport initialization.
 * Used by {@link MessengerTransportBootstrap} to determine which
 * transport to try first and how to handle fallback scenarios.
 */
public enum TransportPriority {
    /**
     * Try Redis first, fall back to plugin messaging if Redis is unavailable.
     * This is the recommended default as Redis provides better performance
     * and reliability for multi-server setups.
     */
    REDIS_FIRST,

    /**
     * Try plugin messaging first, use Redis as a backup.
     * Useful for setups where plugin messaging is preferred or
     * Redis is only available as a secondary option.
     */
    PLUGIN_FIRST,

    /**
     * Use Redis only, fail if Redis is unavailable.
     * Use this when Redis is required and plugin messaging
     * should not be used as a fallback.
     */
    REDIS_ONLY,

    /**
     * Use plugin messaging only.
     * Useful for single-server setups or when Redis is not
     * desired at all.
     */
    PLUGIN_ONLY
}
