package de.t14d3.rapunzelcore.network;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors the health of network transports for observability.
 * Tracks connection status, latency, and failure rates for each transport.
 * <p>
 * This class is thread-safe and uses ConcurrentHashMap for internal state.
 */
public class NetworkHealthMonitor implements AutoCloseable {

    private final Map<String, ConnectionHealth> healthMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> failureCounters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> cleanupTask;
    private final Duration cleanupInterval;

    /**
     * Creates a new NetworkHealthMonitor.
     *
     * @param scheduler The scheduler to use for cleanup tasks
     */
    public NetworkHealthMonitor(ScheduledExecutorService scheduler) {
        this(scheduler, Duration.ofMinutes(1));
    }

    /**
     * Creates a new NetworkHealthMonitor with a custom cleanup interval.
     *
     * @param scheduler The scheduler to use for cleanup tasks
     * @param cleanupInterval How often to reset failure counters
     */
    public NetworkHealthMonitor(ScheduledExecutorService scheduler, Duration cleanupInterval) {
        this.scheduler = scheduler;
        this.cleanupInterval = cleanupInterval;
        this.cleanupTask = startCleanupTask();
    }

    /**
     * Records a successful operation for a transport.
     *
     * @param transport The transport name (e.g., "redis", "plugin-messaging")
     * @param latencyMs The latency of the operation in milliseconds
     */
    public void recordSuccess(String transport, long latencyMs) {
        long currentTime = System.currentTimeMillis();
        healthMap.compute(transport, (k, v) -> {
            if (v == null) {
                return new ConnectionHealth(transport, true, latencyMs, currentTime, 0);
            }
            return new ConnectionHealth(
                    transport,
                    true,
                    calculateAverageLatency(v.latencyMs(), latencyMs),
                    currentTime,
                    0
            );
        });
        failureCounters.put(transport, 0);
    }

    /**
     * Records a failed operation for a transport.
     *
     * @param transport The transport name
     */
    public void recordFailure(String transport) {
        long currentTime = System.currentTimeMillis();
        int failures = failureCounters.merge(transport, 1, Integer::sum);

        healthMap.compute(transport, (k, v) -> {
            if (v == null) {
                return new ConnectionHealth(transport, false, -1, 0, failures);
            }
            return new ConnectionHealth(
                    transport,
                    false,
                    -1,
                    v.lastSuccess(),
                    failures
            );
        });
    }

    /**
     * Updates the connected status of a transport.
     *
     * @param transport The transport name
     * @param connected Whether the transport is connected
     */
    public void updateConnectedStatus(String transport, boolean connected) {
        healthMap.computeIfPresent(transport, (k, v) ->
                new ConnectionHealth(
                        transport,
                        connected,
                        v.latencyMs(),
                        v.lastSuccess(),
                        v.failuresLastMinute()
                )
        );
    }

    /**
     * Gets the current health for a specific transport.
     *
     * @param transport The transport name
     * @return The connection health, or null if no data exists
     */
    public ConnectionHealth getHealth(String transport) {
        return healthMap.get(transport);
    }

    /**
     * Gets health information for all transports.
     *
     * @return A map of transport names to their health status
     */
    public Map<String, ConnectionHealth> getAllHealth() {
        return Map.copyOf(healthMap);
    }

    /**
     * Gets the set of all monitored transport names.
     *
     * @return Set of transport names
     */
    public Set<String> getMonitoredTransports() {
        return Set.copyOf(healthMap.keySet());
    }

    /**
     * Checks if any transport is currently healthy (connected).
     *
     * @return true if at least one transport is connected
     */
    public boolean hasHealthyTransport() {
        return healthMap.values().stream()
                .anyMatch(ConnectionHealth::connected);
    }

    /**
     * Gets the healthiest transport based on latency and failure rate.
     *
     * @return The name of the healthiest transport, or null if none are healthy
     */
    public String getHealthiestTransport() {
        return healthMap.values().stream()
                .filter(ConnectionHealth::connected)
                .min((a, b) -> {
                    // Prioritize by failures first, then latency
                    int failureCompare = Integer.compare(a.failuresLastMinute(), b.failuresLastMinute());
                    if (failureCompare != 0) return failureCompare;
                    return Long.compare(a.latencyMs(), b.latencyMs());
                })
                .map(ConnectionHealth::transport)
                .orElse(null);
    }

    private long calculateAverageLatency(long previousAvg, long newLatency) {
        // Simple exponential moving average
        if (previousAvg < 0) return newLatency;
        return (previousAvg * 3 + newLatency) / 4;
    }

    private ScheduledFuture<?> startCleanupTask() {
        return scheduler.scheduleAtFixedRate(
                this::cleanup,
                cleanupInterval.toMillis(),
                cleanupInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void cleanup() {
        // Reset failure counters periodically
        failureCounters.replaceAll((k, v) -> 0);

        // Update health records to reflect reset counters
        long currentTime = System.currentTimeMillis();
        healthMap.replaceAll((transport, health) ->
                new ConnectionHealth(
                        transport,
                        health.connected(),
                        health.latencyMs(),
                        health.lastSuccess(),
                        0
                )
        );
    }

    @Override
    public void close() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel(false);
        }
        healthMap.clear();
        failureCounters.clear();
    }

    /**
     * Represents the health status of a network transport.
     *
     * @param transport The transport name
     * @param connected Whether the transport is currently connected
     * @param latencyMs The average latency in milliseconds (-1 if unknown)
     * @param lastSuccess Timestamp of the last successful operation
     * @param failuresLastMinute Number of failures in the last minute
     */
    public record ConnectionHealth(
            String transport,
            boolean connected,
            long latencyMs,
            long lastSuccess,
            int failuresLastMinute
    ) {
        /**
         * Checks if the transport is considered healthy.
         * A transport is healthy if it's connected and hasn't had too many failures.
         *
         * @return true if the transport is healthy
         */
        public boolean isHealthy() {
            return connected && failuresLastMinute < 5;
        }

        /**
         * Gets a human-readable status string.
         *
         * @return Status description
         */
        public String getStatusDescription() {
            if (!connected) return "DISCONNECTED";
            if (failuresLastMinute >= 5) return "DEGRADED";
            if (failuresLastMinute > 0) return "WARNING";
            return "HEALTHY";
        }

        @Override
        public String toString() {
            return String.format(
                    "ConnectionHealth[transport=%s, status=%s, latency=%dms, lastSuccess=%d, failures=%d]",
                    transport,
                    getStatusDescription(),
                    latencyMs,
                    lastSuccess,
                    failuresLastMinute
            );
        }
    }
}
