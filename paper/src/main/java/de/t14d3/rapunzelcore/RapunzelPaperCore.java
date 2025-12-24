package de.t14d3.rapunzelcore;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;
import de.t14d3.rapunzellib.network.info.NetworkInfoClient;
import de.t14d3.rapunzellib.platform.paper.PaperRapunzelBootstrap;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;
import de.t14d3.rapunzelcore.commands.CoreCommand;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.Home;
import de.t14d3.rapunzelcore.database.entities.PendingTeleport;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.Warp;
import de.t14d3.rapunzelcore.database.entities.TeleportRequest;
import de.t14d3.rapunzellib.network.queue.DbQueuedMessenger;
import de.t14d3.rapunzellib.network.queue.NetworkOutboxMessage;
import de.t14d3.rapunzellib.network.queue.NetworkQueueConfig;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import de.t14d3.rapunzelcore.modules.chat.PaperChatModuleImpl;
import de.t14d3.rapunzelcore.configsync.CoreConfigSync;
import de.t14d3.rapunzelcore.util.Closeables;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
 import org.bukkit.Bukkit;
 import org.bukkit.permissions.Permission;
 import org.bukkit.permissions.PermissionDefault;
 import org.bukkit.plugin.PluginManager;
 import org.bukkit.plugin.java.JavaPlugin;


import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RapunzelPaperCore extends JavaPlugin implements RapunzelCore {
    private MessageHandler messages;
    private static RapunzelPaperCore instance;
    private SpoolDatabase coreDatabase;
    private YamlConfig config;
    private Messenger messenger;
    private AutoCloseable networking;

    public static final boolean DEBUG = false;

    private static String serverName = null;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        CoreContext.setInstance(this);
        PaperRapunzelBootstrap.bootstrap(this);
        messages = new MessageHandler();

        this.config = Rapunzel.context().configs().load(
            Rapunzel.context().dataDirectory().resolve("config.yml"),
            "config.yml"
        );
        Rapunzel.context().services().register(RapunzelCore.class, this);       

        String jdbc = config.getString("database.jdbc", "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db");
        getLogger().info("Using JDBC: " + jdbc);
        coreDatabase = SpoolDatabase.open(jdbc, getSLF4JLogger(), PlayerEntity.class, Home.class, Warp.class, Channel.class, TeleportRequest.class, PendingTeleport.class, NetworkOutboxMessage.class);
        CoreDatabase.init(coreDatabase);

        Rapunzel.context().services().register(SpoolDatabase.class, coreDatabase);
        setupNetworking();
        CoreDatabase.startEntitySync(messenger);

        CoreConfigSync.bootstrap(this);

        // Load modules from config
        loadModules();

        new CoreCommand();
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(
                new CommandAPIPaperConfig(this)
                        .verboseOutput(DEBUG)
                        .silentLogs(false)
        );
        instance = this;
        CoreContext.setInstance(this);
    }

    @Override
    public void onDisable() {
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
    public YamlConfig getConfiguration() {
        return config;
    }

    @Override
    public PlatformManager getPlatformManager() {
        return new PaperPlatformManager();
    }

    public static class PaperPlatformManager implements PlatformManager {       
        @Override
        public ChatModule.ChatModuleImpl createChatModuleImpl(RapunzelCore core, ChannelManager channelManager) {
            return new PaperChatModuleImpl(core, channelManager);
        }

        @Override
        public void registerPermissions(Map<String, String> permissions) {      
            if (permissions == null || permissions.isEmpty()) return;

            PluginManager pluginManager = Bukkit.getPluginManager();
            for (Map.Entry<String, String> entry : permissions.entrySet()) {
                String permission = entry.getKey();
                if (permission == null || permission.isBlank()) continue;

                PermissionDefault permissionDefault = parsePermissionDefault(entry.getValue());
                Permission existing = pluginManager.getPermission(permission);
                if (existing != null) {
                    existing.setDefault(permissionDefault);
                    continue;
                }
                try {
                    pluginManager.addPermission(new Permission(permission, permissionDefault));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        private static PermissionDefault parsePermissionDefault(String raw) {
            if (raw == null) return PermissionDefault.OP;

            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "true" -> PermissionDefault.TRUE;
                case "false" -> PermissionDefault.FALSE;
                case "notop", "not_op", "not op" -> PermissionDefault.NOT_OP;
                default -> PermissionDefault.OP;
            };
        }

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
    public Environment getEnvironment() {
        return Environment.PAPER;
    }


    public void reloadPlugin() {
        // Disable all modules
        for (Module module : ModuleManager.getModules()) {
            getLogger().info("Disabling module: " + module.getName());
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
                    module.enable(this, getEnvironment());
                    getLogger().info("Loaded module: " + name);
                }
            } catch (Exception e) {
                getLogger().warning("Failed to load module " + clazz.getName() + ": " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        });
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



    public static String getServerName() {
        String cached = serverName;
        if (cached != null) {
            String trimmed = cached.trim();
            if (!trimmed.isBlank() && !"unknown".equalsIgnoreCase(trimmed)) return trimmed;
        }

        try {
            if (instance != null && instance.config != null) {
                String configured = instance.config.getString("network.serverName", "");
                if (configured != null && !configured.isBlank()) {
                    serverName = configured.trim();
                    return serverName;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            if (Rapunzel.isBootstrapped()) {
                Messenger current = Rapunzel.context().services().get(Messenger.class);
                String name = current != null ? current.getServerName() : null;
                if (name != null) {
                    String trimmed = name.trim();
                    if (!trimmed.isBlank() && !"unknown".equalsIgnoreCase(trimmed)) {
                        serverName = trimmed;
                        return serverName;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            String resolved =
                Rapunzel.context().services().get(NetworkInfoService.class).networkServerName().join();
            if (resolved != null && !resolved.isBlank()) {
                serverName = resolved.trim();
                return serverName;
            }
        } catch (Exception ignored) {
        }

        return "unknown";
    }

    public static void setServerName(String serverName) {
        RapunzelPaperCore.serverName = serverName;
    }

    private void setupNetworking() {
        var logger = Rapunzel.context().logger();
        MessengerTransportBootstrap.Result result =
            MessengerTransportBootstrap.bootstrap(config, Rapunzel.context().platformId(), logger);

        AutoCloseable closeable = result.closeable();
        messenger = result.messenger();

        if (result.usingRedis()) {
            NetworkInfoClient info = new NetworkInfoClient(
                messenger,
                Rapunzel.context().scheduler(),
                logger
            );
            Rapunzel.context().services().register(NetworkInfoService.class, info);
            Rapunzel.context().services().register(NetworkInfoClient.class, info);
            closeable = Closeables.chain(closeable, info);
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
                    queueConfig.maxAge()
                );

                Rapunzel.context().services().register(Messenger.class, queued);
                messenger = queued;
                closeable = Closeables.chain(closeable, queued);
            }
        }

        networking = closeable;
    }
}
