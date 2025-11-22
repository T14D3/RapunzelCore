package de.t14d3.core.modules;

import de.t14d3.core.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class JoinLeaveModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private FileConfiguration config;
    private String joinMessage;
    private String leaveMessage;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;

        // Load config
        config = loadConfig(plugin);
        loadMessages();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(new JoinLeaveListener(this), plugin);
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        enabled = false;
        saveMessages();
        saveConfig(plugin, config);
    }

    @Override
    public String getName() {
        return "joinleave";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void sendJoinMessage(Player player) {
        if (joinMessage != null && !joinMessage.trim().isEmpty()) {
            Component message = miniMessage.deserialize(joinMessage, parsed("player", player.getName()));
            plugin.getServer().broadcast(message);
        }
    }

    public void sendLeaveMessage(Player player) {
        if (leaveMessage != null && !leaveMessage.trim().isEmpty()) {
            Component message = miniMessage.deserialize(leaveMessage, parsed("player", player.getName()));
            plugin.getServer().broadcast(message);
        }
    }

    public void setJoinMessage(String message) {
        joinMessage = message != null ? message.trim() : "";
        config.set("join-message", joinMessage);
    }

    public void setLeaveMessage(String message) {
        leaveMessage = message != null ? message.trim() : "";
        config.set("leave-message", leaveMessage);
    }

    private void loadMessages() {
        joinMessage = config.getString("join-message", "");
        leaveMessage = config.getString("leave-message", "");
    }

    private void saveMessages() {
        config.set("join-message", joinMessage);
        config.set("leave-message", leaveMessage);
    }

    private static class JoinLeaveListener implements Listener {
        private final JoinLeaveModule module;

        public JoinLeaveListener(JoinLeaveModule module) {
            this.module = module;
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            // Suppress default join message
            event.joinMessage(null);
            module.sendJoinMessage(event.getPlayer());
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            // Suppress default quit message
            event.quitMessage(null);
            module.sendLeaveMessage(event.getPlayer());
        }
    }
}
