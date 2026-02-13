package de.t14d3.rapunzelcore.network;

import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.platform.paper.network.PaperPluginMessenger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.List;

/**
 * Bootstrap logic for initializing network transports based on configured priority.
 * Supports Redis and plugin messaging with automatic failover between transports.
 * <p>
 * Example usage:
 * <pre>
 * TransportResult result = MessengerTransportBootstrap.bootstrap(
 *     config,
 *     TransportPriority.REDIS_FIRST,
 *     plugin,
 *     logger,
 *     redisMessengerFactory
 * );
 *
 * if (result.isSuccess()) {
 *     Messenger messenger = result.getMessenger();
 *     // Use messenger...
 * }
 * </pre>
 */
public class MessengerTransportBootstrap {

    private static final String REDIS_ENABLED_PATH = "network.redis.enabled";
    private static final String PLUGIN_CHANNEL_PATH = "network.plugin-messaging.channel";
    private static final String TRANSPORT_PRIORITY_PATH = "network.transport-priority";

    private static final String DEFAULT_PLUGIN_CHANNEL = "rapunzel:main";

    /**
     * Factory interface for creating Redis messenger instances.
     * This is provided by RapunzelLib and handles the actual Redis connection.
     */
    @FunctionalInterface
    public interface RedisMessengerFactory {
        /**
         * Creates a Redis messenger from configuration.
         *
         * @param config The YAML configuration
         * @param plugin The plugin instance
         * @param logger The logger
         * @return A Redis messenger instance, or null if creation failed
         */
        Messenger create(YamlConfig config, Plugin plugin, Logger logger);
    }

    /**
     * Bootstraps the messenger transport based on configuration.
     *
     * @param config The YAML configuration
     * @param plugin The plugin instance
     * @param logger The logger
     * @param redisFactory Factory for creating Redis messenger (from RapunzelLib)
     * @return TransportResult containing the messenger and status information
     */
    public static TransportResult bootstrap(YamlConfig config, Plugin plugin, Logger logger,
                                            RedisMessengerFactory redisFactory) {
        TransportPriority priority = readPriority(config, logger);
        return bootstrap(config, priority, plugin, logger, redisFactory);
    }

    /**
     * Bootstraps the messenger transport with explicit priority.
     *
     * @param config The YAML configuration
     * @param priority The transport priority to use
     * @param plugin The plugin instance
     * @param logger The logger
     * @param redisFactory Factory for creating Redis messenger (from RapunzelLib)
     * @return TransportResult containing the messenger and status information
     */
    public static TransportResult bootstrap(YamlConfig config, TransportPriority priority, Plugin plugin,
                                            Logger logger, RedisMessengerFactory redisFactory) {
        logger.info("Bootstrapping network transport with priority: {}", priority);

        return switch (priority) {
            case REDIS_FIRST -> tryRedisThenPlugin(config, plugin, logger, redisFactory);
            case PLUGIN_FIRST -> tryPluginThenRedis(config, plugin, logger, redisFactory);
            case REDIS_ONLY -> createRedisOnly(config, plugin, logger, redisFactory);
            case PLUGIN_ONLY -> createPluginOnly(config, plugin, logger);
        };
    }

    /**
     * Tries Redis first, falls back to plugin messaging if Redis is unavailable.
     *
     * @param config The YAML configuration
     * @param plugin The plugin instance
     * @param logger The logger
     * @param redisFactory Factory for creating Redis messenger
     * @return TransportResult with the created messenger
     */
    public static TransportResult tryRedisThenPlugin(YamlConfig config, Plugin plugin, Logger logger,
                                                     RedisMessengerFactory redisFactory) {
        TransportResult.Builder resultBuilder = TransportResult.builder();
        List<AutoCloseable> closeables = new ArrayList<>();

        // Try Redis first
        RedisResult redisResult = tryCreateRedisMessenger(config, plugin, logger, redisFactory);
        resultBuilder.addAttempt(redisResult.attempt());

        if (redisResult.messenger() != null) {
            // Try to create plugin messaging as fallback
            PluginResult pluginResult = tryCreatePluginMessenger(config, plugin, logger);
            resultBuilder.addAttempt(pluginResult.attempt());

            if (pluginResult.messenger() != null) {
                // Create composite with Redis as primary
                CompositeMessenger composite = new CompositeMessenger.Builder()
                        .primary(redisResult.messenger())
                        .fallback(pluginResult.messenger())
                        .logger(logger)
                        .build();

                return resultBuilder
                        .messenger(composite)
                        .closeable(combineCloseables(closeables))
                        .success(true)
                        .usingRedis(true)
                        .usingPluginMessaging(true)
                        .addWarning("Using plugin messaging as fallback to Redis")
                        .build();
            }

            // Redis only
            return resultBuilder
                    .messenger(redisResult.messenger())
                    .closeable(combineCloseables(closeables))
                    .success(true)
                    .usingRedis(true)
                    .addWarning("Plugin messaging unavailable, using Redis only")
                    .build();
        }

        // Redis failed, try plugin messaging
        logger.warn("Redis unavailable, falling back to plugin messaging");
        PluginResult pluginResult = tryCreatePluginMessenger(config, plugin, logger);
        resultBuilder.addAttempt(pluginResult.attempt());

        if (pluginResult.messenger() != null) {
            return resultBuilder
                    .messenger(pluginResult.messenger())
                    .closeable(pluginResult.closeable())
                    .success(true)
                    .usingPluginMessaging(true)
                    .addWarning("Redis unavailable, using plugin messaging as fallback")
                    .build();
        }

        // Both failed
        return resultBuilder
                .success(false)
                .addError("Both Redis and plugin messaging are unavailable")
                .build();
    }

    /**
     * Tries plugin messaging first, falls back to Redis if unavailable.
     *
     * @param config The YAML configuration
     * @param plugin The plugin instance
     * @param logger The logger
     * @param redisFactory Factory for creating Redis messenger
     * @return TransportResult with the created messenger
     */
    public static TransportResult tryPluginThenRedis(YamlConfig config, Plugin plugin, Logger logger,
                                                     RedisMessengerFactory redisFactory) {
        TransportResult.Builder resultBuilder = TransportResult.builder();

        // Try plugin messaging first
        PluginResult pluginResult = tryCreatePluginMessenger(config, plugin, logger);
        resultBuilder.addAttempt(pluginResult.attempt());

        if (pluginResult.messenger() != null) {
            // Try to create Redis as fallback
            RedisResult redisResult = tryCreateRedisMessenger(config, plugin, logger, redisFactory);
            resultBuilder.addAttempt(redisResult.attempt());

            if (redisResult.messenger() != null) {
                // Create composite with plugin messaging as primary
                CompositeMessenger composite = new CompositeMessenger.Builder()
                        .primary(pluginResult.messenger())
                        .fallback(redisResult.messenger())
                        .logger(logger)
                        .build();

                return resultBuilder
                        .messenger(composite)
                        .closeable(pluginResult.closeable())
                        .success(true)
                        .usingPluginMessaging(true)
                        .usingRedis(true)
                        .build();
            }

            // Plugin messaging only
            return resultBuilder
                    .messenger(pluginResult.messenger())
                    .closeable(pluginResult.closeable())
                    .success(true)
                    .usingPluginMessaging(true)
                    .addWarning("Redis unavailable, using plugin messaging only")
                    .build();
        }

        // Plugin messaging failed, try Redis
        logger.warn("Plugin messaging unavailable, falling back to Redis");
        RedisResult redisResult = tryCreateRedisMessenger(config, plugin, logger, redisFactory);
        resultBuilder.addAttempt(redisResult.attempt());

        if (redisResult.messenger() != null) {
            return resultBuilder
                    .messenger(redisResult.messenger())
                    .success(true)
                    .usingRedis(true)
                    .addWarning("Plugin messaging unavailable, using Redis as fallback")
                    .build();
        }

        // Both failed
        return resultBuilder
                .success(false)
                .addError("Both plugin messaging and Redis are unavailable")
                .build();
    }

    /**
     * Creates a Redis-only messenger.
     *
     * @param config The YAML configuration
     * @param plugin The plugin instance
     * @param logger The logger
     * @param redisFactory Factory for creating Redis messenger
     * @return TransportResult with the Redis messenger
     */
    public static TransportResult createRedisOnly(YamlConfig config, Plugin plugin, Logger logger,
                                                  RedisMessengerFactory redisFactory) {
        TransportResult.Builder resultBuilder = TransportResult.builder();

        RedisResult redisResult = tryCreateRedisMessenger(config, plugin, logger, redisFactory);
        resultBuilder.addAttempt(redisResult.attempt());

        if (redisResult.messenger() != null) {
            return resultBuilder
                    .messenger(redisResult.messenger())
                    .success(true)
                    .usingRedis(true)
                    .build();
        }

        return resultBuilder
                .success(false)
                .addError("Redis is not available (REDIS_ONLY mode)")
                .build();
    }

    /**
     * Creates a plugin messaging-only messenger.
     *
     * @param config The YAML configuration
     * @param plugin The plugin instance
     * @param logger The logger
     * @return TransportResult with the plugin messenger
     */
    public static TransportResult createPluginOnly(YamlConfig config, Plugin plugin, Logger logger) {
        TransportResult.Builder resultBuilder = TransportResult.builder();

        PluginResult pluginResult = tryCreatePluginMessenger(config, plugin, logger);
        resultBuilder.addAttempt(pluginResult.attempt());

        if (pluginResult.messenger() != null) {
            return resultBuilder
                    .messenger(pluginResult.messenger())
                    .closeable(pluginResult.closeable())
                    .success(true)
                    .usingPluginMessaging(true)
                    .build();
        }

        return resultBuilder
                .success(false)
                .addError("Plugin messaging is not available (PLUGIN_ONLY mode)")
                .build();
    }

    private static RedisResult tryCreateRedisMessenger(YamlConfig config, Plugin plugin, Logger logger,
                                                       RedisMessengerFactory redisFactory) {
        boolean enabled = config.getBoolean(REDIS_ENABLED_PATH, false);

        if (!enabled) {
            return new RedisResult(
                    null,
                    TransportResult.TransportAttempt.skipped("redis", "Redis is disabled in config")
            );
        }

        if (redisFactory == null) {
            return new RedisResult(
                    null,
                    TransportResult.TransportAttempt.skipped("redis", "Redis factory not provided")
            );
        }

        try {
            long startTime = System.currentTimeMillis();

            Messenger redisMessenger = redisFactory.create(config, plugin, logger);

            if (redisMessenger == null) {
                return new RedisResult(
                        null,
                        TransportResult.TransportAttempt.failure("redis", "Factory returned null")
                );
            }

            long latency = System.currentTimeMillis() - startTime;

            logger.info("Connected to Redis (latency: {}ms)", latency);

            return new RedisResult(
                    redisMessenger,
                    TransportResult.TransportAttempt.success(
                            "redis",
                            "Connected successfully",
                            latency
                    )
            );

        } catch (Exception e) {
            logger.warn("Failed to connect to Redis: {}", e.getMessage());
            return new RedisResult(
                    null,
                    TransportResult.TransportAttempt.failure("redis", e.getMessage())
            );
        }
    }

    private static PluginResult tryCreatePluginMessenger(YamlConfig config, Plugin plugin, Logger logger) {
        // Check if we're likely behind a proxy (online mode disabled)
        boolean likelyBehindProxy = !Bukkit.getOnlineMode();

        String channel = config.getString(PLUGIN_CHANNEL_PATH, DEFAULT_PLUGIN_CHANNEL);

        try {
            long startTime = System.currentTimeMillis();

            PaperPluginMessenger messenger = new PaperPluginMessenger((JavaPlugin) plugin);

            long latency = System.currentTimeMillis() - startTime;

            logger.info("Initialized plugin messaging on channel '{}' (latency: {}ms)", channel, latency);

            if (!likelyBehindProxy) {
                logger.warn("Server appears to be in online mode. Plugin messaging may not work correctly without a proxy.");
            }

            return new PluginResult(
                    messenger,
                    messenger,
                    TransportResult.TransportAttempt.success(
                            "plugin-messaging",
                            String.format("Initialized on channel %s", channel),
                            latency
                    )
            );

        } catch (Exception e) {
            logger.warn("Failed to initialize plugin messaging: {}", e.getMessage());
            return new PluginResult(
                    null,
                    null,
                    TransportResult.TransportAttempt.failure("plugin-messaging", e.getMessage())
            );
        }
    }

    private static TransportPriority readPriority(YamlConfig config, Logger logger) {
        String priorityStr = config.getString(TRANSPORT_PRIORITY_PATH, "REDIS_FIRST");
        try {
            return TransportPriority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (logger != null) {
                logger.warn("Invalid transport priority '{}', defaulting to REDIS_FIRST", priorityStr);
            }
            return TransportPriority.REDIS_FIRST;
        }
    }

    private static AutoCloseable combineCloseables(List<AutoCloseable> closeables) {
        if (closeables.isEmpty()) {
            return null;
        }
        if (closeables.size() == 1) {
            return closeables.get(0);
        }
        return () -> {
            for (AutoCloseable closeable : closeables) {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (Exception e) {
                    // Log but continue closing others
                }
            }
        };
    }

    private record RedisResult(Messenger messenger, TransportResult.TransportAttempt attempt) {
    }

    private record PluginResult(Messenger messenger, AutoCloseable closeable,
                                TransportResult.TransportAttempt attempt) {
    }
}
