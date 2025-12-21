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
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Player;
import de.t14d3.rapunzelcore.messaging.Messenger;
import de.t14d3.rapunzelcore.messaging.VelocityPluginMessenger;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private CoreDatabase coreDatabase;

    private FileConfiguration config;
    private Messenger messenger;

    @Inject
    public RapunzelVelocityCore(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) throws IOException {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            try {
                Files.copy(getClass().getResourceAsStream("/config.yml"), configFile.toPath());
            } catch (Exception e) {
                getLogger().error("Failed to copy default config.yml: {}", e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        instance = this;
        CoreContext.setInstance(this);

        reloadConfig();

        messages = new MessageHandler(this);
        String jdbc = config.getString("database.jdbc", "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db");
        coreDatabase = new CoreDatabase(jdbc);

        // Initialize cross-server messenger (Velocity bridge)
        messenger = new VelocityPluginMessenger(this, server);

        // Load modules from config
        loadModules();

        // Register core command
        CommandManager commandManager = server.getCommandManager();
        CommandMeta coreCommandMeta = commandManager.metaBuilder("rapunzelcore")
            .aliases("rc", "rapunzel")
            .build();
        
        // Create data directory if it doesn't exist
        if (!getDataFolder().exists()) {
            try {
                Files.createDirectories(getDataFolder().toPath());
            } catch (IOException e) {
                getLogger().error("Failed to create data directory: {}", e.getMessage());
            }
        }
        
        // Copy default messages file if it doesn't exist
        File messagesFile = new File(getDataFolder(), "messages.properties");
        if (!messagesFile.exists()) {
            try {
                Files.copy(getClass().getResourceAsStream("/messages.properties"), messagesFile.toPath());
            } catch (Exception e) {
                getLogger().error("Failed to copy default messages.properties: {}", e.getMessage());
            }
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Plugin shutdown logic
    }


    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public CoreDatabase getCoreDatabase() {
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
        try {
            config.save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().error("Failed to save config.yml: {}", e.getMessage());
        }
    }

    @Override
    public void reloadConfig() {
        try {
            this.config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileConfiguration getConfiguration() {
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
        public void sendMessage(Player receiver, Component message) {
            instance.getServer().getPlayer(receiver.getUuid()).ifPresent(player -> player.sendMessage(message));
        }

        @Override
        public boolean hasPermission(Player player, String permission) {
            return instance.getServer()
                .getPlayer(player.getUuid())
                .map(p -> p.hasPermission(permission))
                .orElse(false);
        }
    }

    @Override
    public Environment getEnvironment() {
        return Environment.VELOCITY;
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
        messages.reloadMessages(this);

        // Load modules
        loadModules();
    }

    private void loadModules() {
        ReflectionsUtil.getSubTypes(Module.class).forEach(clazz -> {
            try {
                Module module = clazz.getDeclaredConstructor().newInstance();
                String name = module.getName();
                ModuleManager.register(module);
                if (getConfiguration().isBoolean("modules." + name) && getConfiguration().getBoolean("modules." + name)) {
                    if (module.getEnvironment() == Environment.BOTH || module.getEnvironment() == getEnvironment()) {
                        module.enable(this, getEnvironment());
                        getLogger().info("Loaded module: " + name);
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


}
