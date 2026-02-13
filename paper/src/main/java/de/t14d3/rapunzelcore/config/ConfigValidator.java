package de.t14d3.rapunzelcore.config;

import de.t14d3.rapunzellib.config.YamlConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates configuration values and provides helpful error messages.
 */
public class ConfigValidator {

    private final Logger logger;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public ConfigValidator(Logger logger) {
        this.logger = logger;
    }

    /**
     * Validates the main configuration file.
     * @param config The configuration to validate
     * @return true if configuration is valid, false otherwise
     */
    public boolean validateConfig(YamlConfig config) {
        errors.clear();
        warnings.clear();

        // Validate network configuration
        validateNetworkConfig(config);

        // Validate database configuration
        validateDatabaseConfig(config);

        // Log warnings
        for (String warning : warnings) {
            logger.warn("Configuration warning: " + warning);
        }

        // Log errors
        for (String error : errors) {
            logger.error("Configuration error: " + error);
        }

        return errors.isEmpty();
    }

    private void validateNetworkConfig(YamlConfig config) {
        String transport = config.getString("network.transport", "plugin");
        if (transport == null || transport.isBlank()) {
            errors.add("network.transport cannot be empty");
        } else if (!transport.equals("plugin") && !transport.equals("redis")) {
            errors.add("network.transport must be either 'plugin' or 'redis', got: " + transport);
        }

        // Validate Redis configuration if transport is redis
        if ("redis".equals(transport)) {
            String host = config.getString("network.redis.host", "");
            if (host == null || host.isBlank()) {
                errors.add("network.redis.host cannot be empty when using redis transport");
            }

            int port = config.getInt("network.redis.port", 6379);
            if (port < 1 || port > 65535) {
                errors.add("network.redis.port must be between 1 and 65535, got: " + port);
            }

            int connectTimeout = config.getInt("network.redis.connectTimeoutMillis", 2000);
            if (connectTimeout < 100) {
                warnings.add("network.redis.connectTimeoutMillis is very low (" + connectTimeout + "ms), consider increasing it");
            }

            int socketTimeout = config.getInt("network.redis.socketTimeoutMillis", 5000);
            if (socketTimeout < 100) {
                warnings.add("network.redis.socketTimeoutMillis is very low (" + socketTimeout + "ms), consider increasing it");
            }
        }

        // Validate queue configuration
        boolean queueEnabled = config.getBoolean("network.queue.enabled", false);
        if (queueEnabled) {
            int flushPeriod = config.getInt("network.queue.flushPeriodSeconds", 2);
            if (flushPeriod < 1) {
                warnings.add("network.queue.flushPeriodSeconds is less than 1 second, setting to 1");
            }

            int maxBatchSize = config.getInt("network.queue.maxBatchSize", 200);
            if (maxBatchSize < 1) {
                errors.add("network.queue.maxBatchSize must be at least 1, got: " + maxBatchSize);
            }

            int maxAge = config.getInt("network.queue.maxAgeSeconds", 300);
            if (maxAge < 1) {
                errors.add("network.queue.maxAgeSeconds must be at least 1, got: " + maxAge);
            }
        }
    }

    private void validateDatabaseConfig(YamlConfig config) {
        String jdbc = config.getString("database.jdbc", "");
        if (jdbc == null || jdbc.isBlank()) {
            errors.add("database.jdbc cannot be empty");
        } else if (!jdbc.startsWith("jdbc:")) {
            errors.add("database.jdbc must start with 'jdbc:', got: " + jdbc);
        }
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
}
