package de.t14d3.rapunzelcore.messaging;

/**
 * Interface for handling incoming messages from other servers.
 */
public interface MessageListener {
    
    /**
     * Handle an incoming message.
     * 
     * @param channel The message channel
     * @param data The message data
     * @param serverName The server that sent the message
     */
    void onMessage(String channel, String data, String serverName);
}
