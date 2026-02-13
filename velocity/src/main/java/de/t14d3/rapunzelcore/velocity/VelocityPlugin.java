package de.t14d3.rapunzelcore.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzelcore.RapunzelVelocityCore;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.velocity.handlers.EntityTransferHandler;
import de.t14d3.rapunzelcore.velocity.listener.EntityTransferListener;
import de.t14d3.rapunzelcore.velocity.listener.PortalListener;
import de.t14d3.rapunzellib.network.Messenger;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Velocity plugin wrapper for RapunzelCore proxy-side functionality.
 *
 *
 * This plugin handles cross-server entity transfers, portal synchronization,
 * and pet transfers by registering channel listeners for ENTITY_TRANSFER,
 * PORTALS, and PETS channels.
 *
 *
 *
 * It acts as a bridge between backend servers, forwarding messages
 * and managing pending transfers.
 *
 *
 */
@Plugin(
    id = "rapunzelcore-velocity",
    name = "RapunzelCore-Velocity",
    version = "1.0.0",
    description = "Velocity proxy-side support for RapunzelCore portals and entity transfers",
    authors = {"T14D3"}
)
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private VelocityNetworkBridge networkBridge;
    private EntityTransferHandler entityTransferHandler;
    private EntityTransferListener entityTransferListener;
    private PortalListener portalListener;
    private Messenger messenger;
    
    private static VelocityPlugin instance;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
    }

/**
 * Initializes the Velocity plugin and registers all channel listeners.
 *
 * @param event the proxy initialization event
 */
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Initializing RapunzelCore Velocity proxy-side support...");
        
        // Initialize the network bridge
        this.networkBridge = new VelocityNetworkBridge(server, logger);
        
        // Initialize handlers
        this.entityTransferHandler = new EntityTransferHandler(server, logger, networkBridge);
        
        // Initialize listeners
        this.entityTransferListener = new EntityTransferListener(server, logger, networkBridge);
        this.portalListener = new PortalListener(server, logger, networkBridge);
        
        // Register event listeners
        server.getEventManager().register(this, entityTransferListener);
        server.getEventManager().register(this, portalListener);
        
        // Register channel listeners (will be done once messenger is available)
        server.getScheduler()
            .buildTask(this, this::registerChannelListeners)
            .delay(2, TimeUnit.SECONDS)
            .schedule();
        
        logger.info("RapunzelCore Velocity proxy-side support initialized successfully");
    }

/**
 * Cleans up resources on proxy shutdown.
 *
 * @param event the proxy shutdown event
 */
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Shutting down RapunzelCore Velocity proxy-side support...");
        
        if (networkBridge != null) {
            networkBridge.shutdown();
        }
        
        if (messenger != null && entityTransferHandler != null) {
            messenger.unregisterListener(NetworkChannels.ENTITY_TRANSFER, entityTransferHandler);
        }
        
        logger.info("RapunzelCore Velocity proxy-side support shut down");
    }

/**
 * Registers channel listeners for ENTITY_TRANSFER, PORTALS, and PETS channels.
 * This is called after a short delay to ensure RapunzelCore is fully initialized.
 */
    private void registerChannelListeners() {
        // Try to get messenger from RapunzelCore
        RapunzelVelocityCore core = RapunzelVelocityCore.getInstance();
        if (core != null) {
            this.messenger = core.getMessenger();
        }
        
        if (messenger == null) {
            logger.warn("Messenger not available from RapunzelCore, retrying in 2 seconds...");
            server.getScheduler()
                .buildTask(this, this::registerChannelListeners)
                .delay(2, TimeUnit.SECONDS)
                .schedule();
            return;
        }
        
        // Register channel listeners
        messenger.registerListener(NetworkChannels.ENTITY_TRANSFER, entityTransferHandler);
        messenger.registerListener(NetworkChannels.PORTALS, portalListener);
        messenger.registerListener(NetworkChannels.PETS, entityTransferHandler);
        
        logger.info("Registered channel listeners for: {}, {}, {}", 
            NetworkChannels.ENTITY_TRANSFER, 
            NetworkChannels.PORTALS, 
            NetworkChannels.PETS);
    }

/**
 * Gets the VelocityNetworkBridge instance.
 *
 * @return the network bridge
 */
    public VelocityNetworkBridge getNetworkBridge() {
        return networkBridge;
    }

/**
 * Gets the EntityTransferHandler instance.
 *
 * @return the entity transfer handler
 */
    public EntityTransferHandler getEntityTransferHandler() {
        return entityTransferHandler;
    }

/**
 * Gets the singleton instance of this plugin.
 *
 * @return the plugin instance
 */
    public static VelocityPlugin getInstance() {
        return instance;
    }

/**
 * Gets the ProxyServer instance.
 *
 * @return the proxy server
 */
    public ProxyServer getServer() {
        return server;
    }

/**
 * Gets the logger instance.
 *
 * @return the logger
 */
    public Logger getLogger() {
        return logger;
    }
}
