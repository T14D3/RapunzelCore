package de.t14d3.rapunzelcore;

import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;


/**
 * Core interface that defines the main functionality for RapunzelCore
 * across all platforms (Paper, Velocity, etc.).
 * 
 * This interface provides a unified API for core functionality including
 * logging, configuration, database access, messaging, and module management.
 */
public interface RapunzelCore {

    static Logger logger = LoggerFactory.getLogger(RapunzelCore.class);

    /**
     * Get the logger instance for this platform.
     *
     * @return The logger instance
     */
    static Logger getLogger() {
        return logger;
    }

    static RapunzelCore getInstance() {
        return CoreContext.getInstance();
    }

    /**
     * Get the environment/platform this core is running on.
     * 
     * @return The environment enum (PAPER, VELOCITY, or BOTH)
     */
    Environment getEnvironment();


    /**
     * Get the data folder for this plugin.
     * 
     * @return The data folder file
     */
    File getDataFolder();
    
    /**
     * Get the core database instance.
     * 
     * @return The core database
     */
    SpoolDatabase getCoreDatabase();
    
    /**
     * Get the message handler for this platform.
     * 
     * @return The message handler
     */
     MessageHandler getMessageHandler();
    
    /**
     * Get the messenger for cross-platform communication.
     * 
     * @return The messenger instance
     */
    Messenger getMessenger();

    
    /**
     * Get all registered modules.
     * 
     * @return List of all modules
     */
    List<Module> getModules();
    
    /**
     * Reload the plugin configuration and modules.
     */
    void reloadPlugin();
    
    /**
     * Save the plugin configuration.
     */
    void saveConfig();
    
    /**
     * Reload the plugin configuration.
     */
    void reloadConfig();
    
    /**
     * Get the plugin configuration.
     * @return The configuration object
     */
    YamlConfig getConfiguration();

    PlatformManager getPlatformManager();

    interface PlatformManager {
        ChatModule.ChatModuleImpl createChatModuleImpl(RapunzelCore core, ChannelManager channelManager);

        /**
         * Registers the given permissions with the underlying platform, if supported.
         * <p>
         * Default implementation is a no-op for platforms without permission registration
         */
        default void registerPermissions(Map<String, String> permissions) {
            // no-op by default
        }
    }
}
