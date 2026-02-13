package de.t14d3.rapunzelcore.network;

/**
 * Predefined network channels for cross-server communication.
 */
public final class NetworkChannels {
    private NetworkChannels() {}

    /** Channel for teleportation-related events. */
    public static final String TELEPORTS = "teleports";

    /** Channel for moderation actions. */
    public static final String MODERATION = "moderation";

    /** Channel for entity transfer between servers (portals, pets, etc.). */
    public static final String ENTITY_TRANSFER = "entity_transfer";

    /** Channel for portal events and synchronization. */
    public static final String PORTALS = "portals";

    /** Channel for pet-related events. */
    public static final String PETS = "pets";

    /** Channel for database cache synchronization. */
    public static final String DB_CACHE_EVENT = "db_cache_event";

    /** Channel for inventory synchronization. */
    public static final String INVENTORIES_SYNC = "inventories_sync";

    /** Channel for chat messages. */
    public static final String CHAT_CHANNEL_MESSAGE = "chat_channel_message";

    /** Channel for join/leave broadcasts. */
    public static final String JOIN_LEAVE_BROADCAST = "join_leave_broadcast";

    /** Channel for teleport proxy messages. */
    public static final String TELEPORTS_PROXY = "teleports_proxy";

    /** Channel for teleport backend messages. */
    public static final String TELEPORTS_BACKEND = "teleports_backend";
}
