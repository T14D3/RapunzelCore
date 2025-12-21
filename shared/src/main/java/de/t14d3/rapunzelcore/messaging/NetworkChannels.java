package de.t14d3.rapunzelcore.messaging;

/**
 * Common logical channels for the RapunzelCore network messenger.
 *
 * <p>Transport uses the Minecraft plugin messaging channel {@code rapunzelcore:bridge}.
 * These constants are the *logical* channels that are multiplexed within that transport.</p>
 */
public final class NetworkChannels {

    private NetworkChannels() {
    }

    public static final String CHAT_CHANNEL_MESSAGE = "chat.channel_message";
}
