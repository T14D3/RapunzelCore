package de.t14d3.rapunzelcore;

import de.t14d3.rapunzelcore.commands.CoreCommand;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Player;
import de.t14d3.rapunzelcore.messaging.Messenger;
import de.t14d3.rapunzelcore.messaging.PaperPluginMessenger;
import de.t14d3.rapunzelcore.modules.chat.ChannelManager;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import de.t14d3.rapunzelcore.modules.chat.PaperChatModuleImpl;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
 import net.kyori.adventure.text.Component;
 import org.bukkit.Bukkit;
 import org.bukkit.permissions.Permission;
 import org.bukkit.permissions.PermissionDefault;
 import org.bukkit.plugin.PluginManager;
 import org.bukkit.plugin.java.JavaPlugin;
 import org.simpleyaml.configuration.file.FileConfiguration;
 import org.simpleyaml.configuration.file.YamlConfiguration;

 import java.io.File;
 import java.io.IOException;
 import java.nio.file.Files;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;

public final class RapunzelPaperCore extends JavaPlugin implements RapunzelCore {
    private MessageHandler messages;
    private static RapunzelPaperCore instance;
    private CoreDatabase coreDatabase;
    private FileConfiguration config;
    private Messenger messenger;

    public static final boolean DEBUG = true;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        CoreContext.setInstance(this);
        messages = new MessageHandler(this);
        saveDefaultConfig();
        try {
            this.config = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String jdbc = config.getString("database.jdbc", "jdbc:sqlite:plugins/RapunzelCore/rapunzelcore.db");
        coreDatabase = new CoreDatabase(jdbc);

        // Initialize cross-server messenger (Velocity bridge)
        messenger = new PaperPluginMessenger(this);

        // Load modules from config
        loadModules();

        new CoreCommand();


        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File messagesFile = new File(getDataFolder(), "messages.properties");
        if (!messagesFile.exists()) {
            try {
                //noinspection DataFlowIssue // File is always included in jar
                Files.copy(getClass().getResourceAsStream("/messages.properties"), messagesFile.toPath());
            } catch (Exception e) {
                getLogger().severe("Failed to copy default messages.properties: " + e.getMessage());
            }
        }
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(
                new CommandAPIPaperConfig(this)
                        .verboseOutput(true)
                        .silentLogs(false)
        );
        instance = this;
        CoreContext.setInstance(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    @Override
    public FileConfiguration getConfiguration() {
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
        public void sendMessage(Player player, Component message) {
            org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            if (bukkitPlayer == null) return;
            bukkitPlayer.sendMessage(message);
        }

        @Override
        public boolean hasPermission(Player player, String permission) {
            org.bukkit.entity.Player bukkitPlayer = Bukkit.getPlayer(player.getUuid());
            if (bukkitPlayer == null) return false;
            return bukkitPlayer.hasPermission(permission);
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
        try {
            if (config != null) {
                config.save(new File(getDataFolder(), "config.yml"));
            }
        } catch (Exception e) {
            getLogger().severe("Failed to save config.yml: " + e.getMessage());
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
}
