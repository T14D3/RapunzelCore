package de.t14d3.rapunzelcore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzellib.platform.velocity.VelocityRapunzelBootstrap;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoResponder;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityNetworkInfoService;
import de.t14d3.rapunzellib.platform.velocity.network.VelocityPluginMessenger;
import de.t14d3.rapunzelcore.configsync.CoreConfigSync;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.Home;
import de.t14d3.rapunzelcore.database.entities.PendingTeleport;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.Warp;
import de.t14d3.rapunzelcore.database.entities.TeleportRequest;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule;
import de.t14d3.rapunzelcore.modules.joinleave.VelocityJoinLeaveModuleImpl;
import de.t14d3.rapunzellib.network.queue.DbQueuedMessenger;
import de.t14d3.rapunzellib.network.queue.NetworkOutboxMessage;
import de.t14d3.rapunzellib.network.queue.NetworkQueueConfig;
import de.t14d3.rapunzelcore.util.Closeables;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Plugin(
    id = "rapunzelcore",
    name = "RapunzelCore",
    version = "1.0.0",
    description = "Core plugin for Rapunzel network",
    authors = {"T14D3"}
)
public class RapunzelVelocityCore implements RapunzelCore {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private MessageHandler messages;
    private static RapunzelVelocityCore instance;
    private SpoolDatabase coreDatabase;

    private YamlConfig config;
    private Messenger messenger;
    private AutoCloseable networking;

    @Inject
    public RapunzelVelocityCore(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        instance = this;
        CoreContext.setInstance(this);

        VelocityRapunzelBootstrap.bootstrap(this, server, logger, dataDirectory);
        Rapunzel.context().services().register(RapunzelCore.class, this);       

        reloadConfig();
        messages = new MessageHandler();
        String jdbc = config.getString("database.jdbc", "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load MySQL driver", e);
            throw new RuntimeException("Failed to load MySQL driver", e);
        }
        coreDatabase = SpoolDatabase.open(jdbc, logger, PlayerEntity.class, Home.class, Warp.class, Channel.class, TeleportRequest.class, PendingTeleport.class, NetworkOutboxMessage.class);
        CoreDatabase.init(coreDatabase);
        Rapunzel.context().services().register(SpoolDatabase.class, coreDatabase);

        setupNetworking();
        CoreDatabase.startEntitySync(messenger);

        CoreConfigSync.bootstrap(this);

        // Load modules from config
        loadModules();

        // Register core command
        CommandManager commandManager = server.getCommandManager();
        CommandMeta coreCommandMeta = commandManager.metaBuilder("rapunzelcore")
            .aliases("rc", "rapunzel")
            .build();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        CoreConfigSync.shutdown();
        AutoCloseable closeable = networking;
        networking = null;
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
        try {
            if (coreDatabase != null) coreDatabase.close();
        } catch (Exception ignored) {
        }
        CoreDatabase.shutdown();
        Rapunzel.shutdown();
    }


    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public SpoolDatabase getCoreDatabase() {
        return coreDatabase;
    }

    @Override
    public MessageHandler getMessageHandler() {
        return messages;
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }


    @Override
    public List<Module> getModules() {
        return ModuleManager.getModules();
    }

    @Override
    public void saveConfig() {
        if (config != null) config.save();
    }

    @Override
    public void reloadConfig() {
        this.config = Rapunzel.context().configs().load(
            Rapunzel.context().dataDirectory().resolve("config.yml"),
            "config.yml"
        );
    }

    @Override
    public YamlConfig getConfiguration() {
        return config;
    }

    @Override
    public PlatformManager getPlatformManager() {
        return new VelocityPlatformManager();
    }

    public ProxyServer getServer() {
        return server;
    }

    public CommandManager getCommandManager() {
        return server.getCommandManager();
    }

    public static class VelocityPlatformManager implements PlatformManager {    
        private static final class NoopChatModuleImpl implements ChatModule.ChatModuleImpl {
            @Override
            public void initialize() {
                // no-op
            }

            @Override
            public void cleanup() {
                // no-op
            }
        }

        @Override
        public ChatModule.ChatModuleImpl createChatModuleImpl(RapunzelCore core, ChannelManager channelManager) {
            // Chat runs on Paper backends. On Velocity we only provide the message router (VelocityPluginMessenger).
            return new NoopChatModuleImpl();
        }

        @Override
        public JoinLeaveModule.JoinLeaveModuleImpl createJoinLeaveModuleImpl(RapunzelCore core, boolean networkEnabled, java.nio.file.Path configPath) {
            return new VelocityJoinLeaveModuleImpl((RapunzelVelocityCore) core, networkEnabled, configPath);
        }

    }

    @Override
    public Environment getEnvironment() {
        return Environment.VELOCITY;
    }






    public void reloadPlugin() {
        // Disable all modules
        for (Module module : ModuleManager.getModules()) {
            getLogger().info("Disabling module: {}", module.getName());
            module.disable(this, getEnvironment());
            ModuleManager.disable(module.getName(), getEnvironment());
        }

        // Reload plugin configuration
        reloadConfig();

        // Reload messages
        messages.reloadMessages();

        // Load modules
        loadModules();
    }

    private void loadModules() {
        ReflectionsUtil.getSubTypes(Module.class).forEach(clazz -> {
            try {
                Module module = clazz.getDeclaredConstructor().newInstance();
                String name = module.getName();
                ModuleManager.register(module);
                if (getConfiguration().getBoolean("modules." + name, false)) {
                    if (module.getEnvironment() == Environment.BOTH || module.getEnvironment() == getEnvironment()) {
                        module.enable(this, getEnvironment());
                        getLogger().info("Loaded module: {}", name);
                    } else {
                        getLogger().info("Skipping module {} (not compatible with {})", name, getEnvironment());
                    }
                }
            } catch (Exception e) {
                getLogger().warn("Failed to load module {}: {}", clazz.getName(), e.getMessage());
                getLogger().debug("Stack trace: {}", (Object) e.getStackTrace());
            }
        });
    }


    public static RapunzelVelocityCore getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    private void setupNetworking() {
        MessengerTransportBootstrap.Result result = MessengerTransportBootstrap.bootstrap(
            config,
            Rapunzel.context().platformId(),
            logger
        );

        AutoCloseable closeable = result.closeable();
        messenger = result.messenger();

        if (result.usingRedis()) {
            VelocityNetworkInfoResponder responder = new VelocityNetworkInfoResponder(
                messenger,
                server,
                logger
            );
            Rapunzel.context().services().register(VelocityNetworkInfoResponder.class, responder);
            closeable = Closeables.chain(closeable, responder);

            VelocityNetworkInfoService networkInfo = new VelocityNetworkInfoService(messenger, server);
            Rapunzel.context().services().register(NetworkInfoService.class, networkInfo);
            Rapunzel.context().services().register(VelocityNetworkInfoService.class, networkInfo);
            } else {
                NetworkQueueConfig queueConfig = NetworkQueueConfig.read(config);
                if (queueConfig.enabled()) {
                    DbQueuedMessenger queued = new DbQueuedMessenger(
                        coreDatabase,
                    messenger,
                    Rapunzel.context().scheduler(),
                    logger,
                    NetworkQueueConfig.defaultOwnerId(),
                    queueConfig.channelAllowlist(),
                    queueConfig.flushPeriod(),
                    queueConfig.maxBatchSize(),
                    queueConfig.maxAge(),
                    this::allBackendServers,
                    this::hasCarrierOnBackend
                );

                    Rapunzel.context().services().register(Messenger.class, queued);
                    messenger = queued;
                    closeable = Closeables.chain(closeable, queued);

                    Rapunzel.context().services()
                        .find(VelocityPluginMessenger.class)
                        .ifPresent(pm -> pm.setUndeliverableForwarder(messenger));
                }
            }

        networking = closeable;
    }

    private List<String> allBackendServers() {
        return server.getAllServers().stream().map(rs -> rs.getServerInfo().getName()).toList();
    }

    private boolean hasCarrierOnBackend(String backendServer) {
        if (backendServer == null || backendServer.isBlank()) return false;
        String needle = backendServer.trim();
        return server.getAllPlayers().stream()
            .anyMatch(p -> p.getCurrentServer()
                .map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(needle))
                .orElse(false));
    }

}
