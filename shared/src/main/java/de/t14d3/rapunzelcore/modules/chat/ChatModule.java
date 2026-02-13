package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.Rapunzel;
import java.nio.file.Path;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.Environment;

/**
 * Generic ChatModule that delegates to platform-specific implementations.
 * This module automatically selects the appropriate implementation based on the environment.
 */
public class ChatModule implements Module {
    private ChannelManager channelManager;
    private static String[] iconConfig;
    private static String defaultFormat;
    private boolean enabled = false;

    // Paper-only: chat is handled on backend servers, not on the proxy.
    private ChatModuleImpl chatImpl;

    public static String getDefaultFormat() {
        return defaultFormat != null ? defaultFormat : "<name>: <message>";
    }

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        YamlConfig config = loadConfig();
        iconConfig = config.getString("general.icons.item", "gui:icon/search").split(":");
        defaultFormat = config.getString("general.fallback-format", "default");

        channelManager = new ChannelManager(config);

        // Create platform-specific implementation
        chatImpl = core.getPlatformManager().createChatModuleImpl(core, channelManager);
        if (chatImpl != null) {
            chatImpl.initialize();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (chatImpl != null) {
            chatImpl.cleanup();
        }
        if (channelManager != null) {
            channelManager.close();
        }
        chatImpl = null;
        channelManager = null;
    }

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
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

    public interface ChatModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();
    }
}
