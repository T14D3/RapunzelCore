package de.t14d3.rapunzelcore;

import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import de.t14d3.rapunzelcore.modules.moderation.ModerationModule;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import de.t14d3.rapunzelcore.modules.entitytransfer.EntityTransferModule;
import de.t14d3.rapunzelcore.modules.portals.PortalModule;
import de.t14d3.rapunzelcore.modules.pets.PetsModule;


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
    List<ModuleDescriptor> getModules();
    
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

    /**
     * Get the resource provider for accessing plugin resources.
     *
     * @return The resource provider
     */
    Object getResourceProvider();
interface PlatformManager {
        ChatModule.ChatModuleImpl createChatModuleImpl(RapunzelCore core, ChannelManager channelManager);
        JoinLeaveModule.JoinLeaveModuleImpl createJoinLeaveModuleImpl(RapunzelCore core, YamlConfig config, java.nio.file.Path configPath);

        /**
         * Creates a platform-specific implementation of the moderation module.
         *
         * @param core The RapunzelCore instance
         * @param config The moderation module configuration
         * @return The platform-specific implementation, or null if not supported
         */
        default ModerationModule.ModerationModuleImpl createModerationModuleImpl(RapunzelCore core, YamlConfig config) {
            return null;
        }

        /**
         * Registers the given permissions with the underlying platform, if supported.
         * <p>
         * Default implementation is a no-op for platforms without permission registration
         */
        default void registerPermissions(Map<String, String> permissions) {
            // no-op by default
        }

        /**
         * Creates a platform-specific implementation of the entity transfer module.
         *
         * @param core The RapunzelCore instance
         * @return The platform-specific implementation, or null if not supported
         */
        default EntityTransferModule.EntityTransferModuleImpl createEntityTransferModuleImpl(RapunzelCore core) {
            return null;
        }

        /**
         * Creates a platform-specific implementation of the portal module.
         *
         * @param core The RapunzelCore instance
         * @return The platform-specific implementation, or null if not supported
         */
        default PortalModule.PortalModuleImpl createPortalModuleImpl(RapunzelCore core) {
            return null;
        }

        /**
         * Creates a platform-specific implementation of the pets module.
         *
         * @param core The RapunzelCore instance
         * @return The platform-specific implementation, or null if not supported
         */
        default PetsModule.PetsModuleImpl createPetsModuleImpl(RapunzelCore core) {
            return null;
        }
    }
}
