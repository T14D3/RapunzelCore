package de.t14d3.core.listeners;

import de.t14d3.core.Main;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class ChatListener implements Listener {
    private final Main plugin;
    private final List<String> commands = List.of(
            "msg",
            "tell",
            "w",
            "r",
            "reply",
            "whisper"
    );

    public ChatListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onChat(AsyncChatEvent event) {

    }

    @EventHandler
    private void onCommand(PlayerCommandPreprocessEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (commands.contains(event.getMessage().split(" ")[0])) {
                Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.hasMetadata("socialspy")) {
                        player.sendMessage(plugin.getMessage("commands.socialspy.message", event.getMessage()));
                    }
                });
            }
        });
    }
}
