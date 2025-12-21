package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.Module;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.util.Map;

/**
 * Generic ChatModule that delegates to platform-specific implementations.
 * This module automatically selects the appropriate implementation based on the environment.
 */
public class ChatModule implements Module {
    private boolean enabled = false;
    private FileConfiguration config;
    private ChannelManager channelManager;
    private static String[] iconConfig;
    private static String defaultFormat;

    // Paper-only: chat is handled on backend servers, not on the proxy.
    private ChatModuleImpl chatImpl;

    public static String getDefaultFormat() {
        return defaultFormat != null ? defaultFormat : "<name>: <message>";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        enabled = true;

        config = loadConfig();
        iconConfig = config.getString("general.icons.item", "gui:icon/search").split(":");
        defaultFormat = config.getString("general.fallback-format", "<name>: <message>");

        channelManager = new ChannelManager(config);

        // Create platform-specific implementation
        chatImpl = core.getPlatformManager().createChatModuleImpl(core, channelManager);
        chatImpl.initialize();
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        
        if (chatImpl != null) {
            chatImpl.cleanup();
        }
        
        enabled = false;
        chatImpl = null;
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.msg", "true"),
                Map.entry("rapunzelcore.broadcast", "op"),
                Map.entry("rapunzelcore.socialspy", "op"),
                Map.entry("rapunzelcore.socialspy.bypass", "op"),
                Map.entry("rapunzelcore.channel", "true"),
                Map.entry("rapunzelcore.chat.color", "false")
        );
    }

    /**
     * Gets the channel manager.
     * @return The channel manager
     */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public static String[] getIconConfig() {
        // Safe fallback for modules that use Utils.itemResolver before ChatModule is enabled.
        if (iconConfig == null || iconConfig.length < 2) {
            return "gui:icon/search".split(":");
        }
        return iconConfig;
    }

    /*
     * Interface for platform-specific chat module implementations.
     */
    public interface ChatModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();
    }
}
