package de.t14d3.rapunzelcore.network;

import de.t14d3.rapunzellib.network.Messenger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of the messenger transport bootstrap process.
 * Contains the created messenger, status information, and details about
 * which transports were tried and their results.
 */
public class TransportResult {

    private final Messenger messenger;
    private final AutoCloseable closeable;
    private final boolean success;
    private final boolean usingRedis;
    private final boolean usingPluginMessaging;
    private final List<String> warnings;
    private final List<String> errors;
    private final List<TransportAttempt> attemptedTransports;

    private TransportResult(Builder builder) {
        this.messenger = builder.messenger;
        this.closeable = builder.closeable;
        this.success = builder.success;
        this.usingRedis = builder.usingRedis;
        this.usingPluginMessaging = builder.usingPluginMessaging;
        this.warnings = List.copyOf(builder.warnings);
        this.errors = List.copyOf(builder.errors);
        this.attemptedTransports = List.copyOf(builder.attemptedTransports);
    }

    /**
     * Gets the created messenger instance.
     *
     * @return The messenger, or null if bootstrap failed
     */
    public Messenger getMessenger() {
        return messenger;
    }

    /**
     * Gets the closeable resource that should be closed on shutdown.
     *
     * @return The closeable resource, may be null
     */
    public AutoCloseable getCloseable() {
        return closeable;
    }

    /**
     * Checks if the bootstrap was successful.
     *
     * @return true if a messenger was successfully created
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Checks if Redis transport is being used.
     *
     * @return true if the messenger uses Redis
     */
    public boolean isUsingRedis() {
        return usingRedis;
    }

    /**
     * Checks if plugin messaging transport is being used.
     *
     * @return true if the messenger uses plugin messaging
     */
    public boolean isUsingPluginMessaging() {
        return usingPluginMessaging;
    }

    /**
     * Gets any warnings that occurred during bootstrap.
     *
     * @return List of warning messages
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Gets any errors that occurred during bootstrap.
     *
     * @return List of error messages
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Gets details about all transport attempts.
     *
     * @return List of transport attempts
     */
    public List<TransportAttempt> getAttemptedTransports() {
        return attemptedTransports;
    }

    /**
     * Creates a new builder for TransportResult.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for TransportResult.
     */
    public static class Builder {
        private Messenger messenger;
        private AutoCloseable closeable;
        private boolean success;
        private boolean usingRedis;
        private boolean usingPluginMessaging;
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private List<TransportAttempt> attemptedTransports = new ArrayList<>();

        public Builder messenger(Messenger messenger) {
            this.messenger = messenger;
            return this;
        }

        public Builder closeable(AutoCloseable closeable) {
            this.closeable = closeable;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder usingRedis(boolean usingRedis) {
            this.usingRedis = usingRedis;
            return this;
        }

        public Builder usingPluginMessaging(boolean usingPluginMessaging) {
            this.usingPluginMessaging = usingPluginMessaging;
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public Builder addAttempt(TransportAttempt attempt) {
            this.attemptedTransports.add(attempt);
            return this;
        }

        public TransportResult build() {
            return new TransportResult(this);
        }
    }

    /**
     * Represents a single transport attempt during bootstrap.
     */
    public record TransportAttempt(
            String transportName,
            boolean attempted,
            boolean successful,
            String message,
            Long latencyMs
    ) {
        /**
         * Creates a successful transport attempt record.
         *
         * @param transportName The name of the transport
         * @param message Success message
         * @param latencyMs Latency in milliseconds
         * @return A successful transport attempt
         */
        public static TransportAttempt success(String transportName, String message, long latencyMs) {
            return new TransportAttempt(transportName, true, true, message, latencyMs);
        }

        /**
         * Creates a failed transport attempt record.
         *
         * @param transportName The name of the transport
         * @param message Failure message
         * @return A failed transport attempt
         */
        public static TransportAttempt failure(String transportName, String message) {
            return new TransportAttempt(transportName, true, false, message, null);
        }

        /**
         * Creates a record for a transport that was not attempted.
         *
         * @param transportName The name of the transport
         * @param message Reason for not attempting
         * @return A skipped transport attempt
         */
        public static TransportAttempt skipped(String transportName, String message) {
            return new TransportAttempt(transportName, false, false, message, null);
        }
    }
}
