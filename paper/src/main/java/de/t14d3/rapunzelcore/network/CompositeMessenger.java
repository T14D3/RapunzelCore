package de.t14d3.rapunzelcore.network;

import de.t14d3.rapunzellib.network.Messenger;
import org.slf4j.Logger;

import java.util.ArrayList;
import de.t14d3.rapunzellib.network.MessageListener;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * A composite messenger that combines multiple messengers with automatic failover.
 * The primary messenger is tried first, and if it fails or is not connected,
 * fallbacks are tried in order.
 * <p>
 * This class is thread-safe for send operations.
 */
public class CompositeMessenger implements Messenger {

    private final List<Messenger> messengers;
    private final Messenger primary;
    private final Logger logger;
    private final NetworkHealthMonitor healthMonitor;

    /**
     * Creates a new CompositeMessenger.
     *
     * @param messengers List of all available messengers (including primary)
     * @param primary The primary messenger to use first
     * @param logger Logger for warnings and errors
     */
    public CompositeMessenger(List<Messenger> messengers, Messenger primary, Logger logger) {
        this(messengers, primary, logger, null);
    }

    /**
     * Creates a new CompositeMessenger with health monitoring.
     *
     * @param messengers List of all available messengers (including primary)
     * @param primary The primary messenger to use first
     * @param logger Logger for warnings and errors
     * @param healthMonitor Optional health monitor for recording success/failure
     */
    public CompositeMessenger(List<Messenger> messengers, Messenger primary, Logger logger, NetworkHealthMonitor healthMonitor) {
        this.messengers = List.copyOf(messengers);
        this.primary = primary;
        this.logger = logger;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public void sendToAll(String channel, String data) {
        // Try primary first
        if (trySendToAll(primary, channel, data)) {
            return;
        }

        logger.warn("Primary messenger failed for channel '{}', trying fallbacks", channel);

        // Try fallbacks in order
        for (Messenger messenger : messengers) {
            if (messenger != primary && trySendToAll(messenger, channel, data)) {
                logger.info("Fallback messenger succeeded for channel '{}'", channel);
                return;
            }
        }

        logger.error("All messengers failed to send message to channel '{}'", channel);
    }

    @Override
    public void sendToServer(String serverId, String channel, String data) {
        // Try primary first
        if (trySendToServer(primary, serverId, channel, data)) {
            return;
        }

        logger.warn("Primary messenger failed sending to server {} on channel '{}', trying fallbacks",
                serverId, channel);

        // Try fallbacks in order
        for (Messenger messenger : messengers) {
            if (messenger != primary && trySendToServer(messenger, serverId, channel, data)) {
                logger.info("Fallback messenger succeeded for server {} on channel '{}'", serverId, channel);
                return;
            }
        }

        logger.error("All messengers failed to send message to server {} on channel '{}'", serverId, channel);
    }

    @Override
    public boolean isConnected() {
        // Consider connected if any messenger is connected
        return messengers.stream().anyMatch(Messenger::isConnected);
    }

    @Override
    public String getServerName() {
        // Try primary first, then fallbacks
        if (primary.isConnected()) {
            return primary.getServerName();
        }

        for (Messenger messenger : messengers) {
            if (messenger.isConnected()) {
                return messenger.getServerName();
            }
        }

        return "unknown";
    }

    @Override
    public String getProxyServerName() {
        // Try primary first, then fallbacks
        if (primary.isConnected()) {
            return primary.getProxyServerName();
        }

        for (Messenger messenger : messengers) {
            if (messenger.isConnected()) {
                return messenger.getProxyServerName();
            }
        }

        return "unknown";
    }

    /**
     * Gets the primary messenger.
     *
     * @return The primary messenger
     */
    public Messenger getPrimary() {
        return primary;
    }

    /**
     * Gets all available messengers.
     *
     * @return List of all messengers
     */
    public List<Messenger> getMessengers() {
        return messengers;
    }

    /**
     * Gets the first connected messenger.
     *
     * @return The first connected messenger, or null if none are connected
     */
    public Messenger getFirstConnected() {
        if (primary.isConnected()) {
            return primary;
        }
        return messengers.stream()
                .filter(Messenger::isConnected)
                .findFirst()
                .orElse(null);
    }

    private boolean trySendToAll(Messenger messenger, String channel, String data) {
        if (!messenger.isConnected()) {
            return false;
        }

        long startTime = System.currentTimeMillis();
        String transportName = getTransportName(messenger);

        try {
            messenger.sendToAll(channel, data);
            recordSuccess(transportName, startTime);
            return true;
        } catch (Exception e) {
            recordFailure(transportName);
            logger.debug("Messenger {} failed to send to all on channel {}: {}",
                    transportName, channel, e.getMessage());
            return false;
        }
    }

    private boolean trySendToServer(Messenger messenger, String serverId, String channel, String data) {
        if (!messenger.isConnected()) {
            return false;
        }

        long startTime = System.currentTimeMillis();
        String transportName = getTransportName(messenger);

        try {
            messenger.sendToServer(serverId, channel, data);
            recordSuccess(transportName, startTime);
            return true;
        } catch (Exception e) {
            recordFailure(transportName);
            logger.debug("Messenger {} failed to send to server {} on channel {}: {}",
                    transportName, serverId, channel, e.getMessage());
            return false;
        }
    }

    private void recordSuccess(String transportName, long startTime) {
        if (healthMonitor != null) {
            long latency = System.currentTimeMillis() - startTime;
            healthMonitor.recordSuccess(transportName, latency);
        }
    }

    private void recordFailure(String transportName) {
        if (healthMonitor != null) {
            healthMonitor.recordFailure(transportName);
        }
    }

    private String getTransportName(Messenger messenger) {
        String className = messenger.getClass().getSimpleName();
        if (className.toLowerCase().contains("redis")) {
            return "redis";
        } else if (className.toLowerCase().contains("plugin")) {
            return "plugin-messaging";
        }
        return className.toLowerCase();
    }

    /**
     * Builder for creating CompositeMessenger instances.
     */
    public static class Builder {
        private final List<Messenger> messengers = new ArrayList<>();
        private Messenger primary;
        private Logger logger;
        private NetworkHealthMonitor healthMonitor;

        public Builder primary(Messenger primary) {
            this.primary = primary;
            if (!messengers.contains(primary)) {
                messengers.add(primary);
            }
            return this;
        }

        public Builder fallback(Messenger fallback) {
            if (!messengers.contains(fallback)) {
                messengers.add(fallback);
            }
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder healthMonitor(NetworkHealthMonitor healthMonitor) {
            this.healthMonitor = healthMonitor;
            return this;
        }

        public CompositeMessenger build() {
            if (primary == null) {
                throw new IllegalStateException("Primary messenger must be set");
            }
            if (logger == null) {
                throw new IllegalStateException("Logger must be set");
            }
            return new CompositeMessenger(messengers, primary, logger, healthMonitor);
        }
    }

    @Override
    public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
        for (Messenger messenger : messengers) {
            messenger.unregisterListener(channel, listener);
        }
    }

    @Override
    public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
        for (Messenger messenger : messengers) {
            messenger.registerListener(channel, listener);
        }
    }

    @Override
    public void sendToProxy(@NotNull String channel, @NotNull String data) {
        for (Messenger messenger : messengers) {
            messenger.sendToProxy(channel, data);
        }
    }
}
