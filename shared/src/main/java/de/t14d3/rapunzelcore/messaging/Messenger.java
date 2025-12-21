package de.t14d3.rapunzelcore.messaging;

/**
 * Interface for cross-server messaging in RapunzelCore.
 * This handles communication between Velocity proxy and Paper backend servers.
 */
public interface Messenger {
    
    /**
     * Send a message to all connected servers in the network.
     * 
     * @param channel The message channel
     * @param data The message data
     */
    void sendToAll(String channel, String data);
    
    /**
     * Send a message to a specific server.
     * 
     * @param channel The message channel
     * @param serverName The target server name
     * @param data The message data
     */
    void sendToServer(String channel, String serverName, String data);
    
    /**
     * Send a message to the authoritative proxy (Velocity).
     * 
     * @param channel The message channel
     * @param data The message data
     */
    void sendToProxy(String channel, String data);
    
    /**
     * Register a message listener for a specific channel.
     * 
     * @param channel The message channel
     * @param listener The message listener
     */
    void registerListener(String channel, MessageListener listener);
    
    /**
     * Unregister a message listener for a specific channel.
     * 
     * @param channel The message channel
     * @param listener The message listener
     */
    void unregisterListener(String channel, MessageListener listener);
    
    /**
     * Check if the messenger is connected to the network.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Get the current server name.
     * 
     * @return The server name
     */
    String getServerName();
    
    /**
     * Get the proxy server name.
     * 
     * @return The proxy server name
     */
    String getProxyServerName();
}
