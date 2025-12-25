package de.t14d3.rapunzelcore.network;

/**
 * Common logical channels for RapunzelCore cross-server messaging.
 *
 * <p>Transport is provided by RapunzelLib. These constants are the logical channels used on that transport.</p>
 */
public final class NetworkChannels {

    private NetworkChannels() {
    }

    public static final String CHAT_CHANNEL_MESSAGE = "chat.channel_message";

    public static final String TELEPORTS_PROXY = "teleports.proxy";
    public static final String TELEPORTS_BACKEND = "teleports.backend";
    public static final String DB_CACHE_EVENT = "db.cache_event";
    public static final String JOIN_LEAVE_BROADCAST = "joinleave.broadcast";
}
