package de.t14d3.rapunzelcore.listeners;

import de.t14d3.rapunzelcore.Main;
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
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("rapunzelcore.socialspy.bypass")) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            commands.forEach(command -> {
                String[] split = event.getMessage().split(" ");
                if (split.length >= 2 && split[0].contains(command)) {
                    String sender = event.getPlayer().getName();
                    String receiver = split[1];
                    String message = new StringBuilder(event.getMessage())
                            .delete(0, split[0].length() + split[1].length() + 2)
                            .toString();
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                        if (player.hasMetadata("socialspy")) {
                            player.sendMessage(plugin.getMessage("commands.socialspy.message", sender, receiver, message));
                        }
                    });
                }
            });
        });
    }

    public void unregister() {
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
    }
}
