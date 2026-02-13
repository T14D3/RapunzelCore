package de.t14d3.rapunzelcore.network;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Centralized network manager for cross-server communication.
 * Provides a unified interface for sending messages across the network.
 */
public final class NetworkManager {

    private NetworkManager() {
    }

    /**
     * Gets the local server name.
     * @return the local server name, or null if not available
     */
    public static String getLocalServerName() {
        if (!Rapunzel.isBootstrapped()) return null;
        try {
            Messenger messenger = Rapunzel.context().services().get(Messenger.class);
            if (messenger == null) return null;
            String name = messenger.getServerName();
            if (name == null) return null;
            String trimmed = name.trim();
            if (trimmed.isBlank() || "unknown".equalsIgnoreCase(trimmed)) return null;
            return trimmed;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Resolves the local server name asynchronously.
     * @return a CompletableFuture that completes with the local server name
     */
    public static CompletableFuture<String> resolveLocalServerName() {
        if (!Rapunzel.isBootstrapped()) return CompletableFuture.completedFuture(null);
        try {
            NetworkInfoService info = Rapunzel.context().services().get(NetworkInfoService.class);
            return info.networkServerName().exceptionally(ignored -> null);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Checks if a server is the local server.
     * @param serverName the server name to check
     * @return true if the server is local
     */
    public static boolean isLocalServer(String serverName) {
        if (serverName == null || serverName.isBlank()) return true;
        String local = getLocalServerName();
        if (local == null) return false;
        return local.equalsIgnoreCase(serverName.trim());
    }

    /**
     * Sends a message to a specific server.
     * @param channel the channel to send on
     * @param targetServer the target server name
     * @param payload the message payload
     * @param messenger the messenger instance
     */
    public static void sendToServer(String channel, String targetServer, Object payload, Messenger messenger) {
        if (channel == null || channel.isBlank()) return;
        if (targetServer == null || targetServer.isBlank()) return;
        if (messenger == null) return;

        if (isLocalServer(targetServer)) {
            // Message is for local server, handle locally
            return;
        }

        new NetworkEventBus(messenger).sendToServer(channel, targetServer, payload);
    }

    /**
     * Sends a message to the proxy.
     * @param channel the channel to send on
     * @param payload the message payload
     * @param messenger the messenger instance
     */
    public static void sendToProxy(String channel, Object payload, Messenger messenger) {
        if (channel == null || channel.isBlank()) return;
        if (messenger == null) return;

        new NetworkEventBus(messenger).sendToProxy(channel, payload);
    }

    /**
     * Sends a message to all servers.
     * @param channel the channel to send on
     * @param payload the message payload
     * @param messenger the messenger instance
     */
    public static void broadcast(String channel, Object payload, Messenger messenger) {
        if (channel == null || channel.isBlank()) return;
        if (messenger == null) return;

        new NetworkEventBus(messenger).sendToAll(channel, payload);
    }

    /**
     * Checks if the network system is ready.
     * @return true if the network system is ready
     */
    public static boolean isNetworkReady() {
        return Rapunzel.isBootstrapped() && getLocalServerName() != null;
    }

    /**
     * Gets the network info service.
     * @return the network info service, or null if not available
     */
    public static NetworkInfoService getNetworkInfoService() {
        if (!Rapunzel.isBootstrapped()) return null;
        try {
            return Rapunzel.context().services().get(NetworkInfoService.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Gets the messenger service.
     * @return the messenger service, or null if not available
     */
    public static Messenger getMessenger() {
        if (!Rapunzel.isBootstrapped()) return null;
        try {
            return Rapunzel.context().services().get(Messenger.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
