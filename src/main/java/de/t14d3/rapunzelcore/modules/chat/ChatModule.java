package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.Module;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

public class ChatModule implements Module {
    private boolean enabled = false;
    private FileConfiguration config;
    private ChatCommands chatCommands;
    private ChatListener chatListener;
    private ChannelManager channelManager;
    private static String[] iconConfig;

    public static final NamespacedKey SOCIALSPY_KEY = new NamespacedKey(Main.getInstance(), "socialspy");

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        enabled = true;

        config = loadConfig();
        iconConfig = config.getString("general.icons.item", "gui:icon/search").split(":");

        channelManager = new ChannelManager(config);

        chatCommands = new ChatCommands(plugin, channelManager);
        chatCommands.register();

        chatListener = new ChatListener(plugin, channelManager);
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        chatCommands.unregister();
        chatListener.unregister();
        enabled = false;
    }

    @Override
    public String getName() {
        return "chat";
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
        return iconConfig;
    }
}
