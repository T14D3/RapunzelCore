package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.chat.channels.Channel;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static de.t14d3.rapunzelcore.modules.chat.ChatModule.SOCIALSPY_KEY;

public class ChatListener implements Listener {
    private final Main plugin;
    private final ChannelManager channelManager;
    private final List<String> commands = List.of(
            "msg",
            "tell",
            "w",
            "r",
            "reply",
            "whisper"
    );

    public ChatListener(Main plugin, ChannelManager channelManager) {
        this.plugin = plugin;
        this.channelManager = channelManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        Channel channel = channelManager.getMainChannel(player);
        event.setCancelled(true);
        channel.sendMessage(player, event.originalMessage());
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
                    String message = event.getMessage()
                            .substring(split[0].length() + split[1].length() + 2);
                    Component socialSpyMessage = plugin.getMessageHandler().getMessage("commands.socialspy.message", sender, receiver, message);
                    Bukkit.getServer().getOnlinePlayers().forEach(player -> {
                        if (player.getPersistentDataContainer().has(SOCIALSPY_KEY)) {
                            player.sendMessage(socialSpyMessage);
                        }
                    });
                }
            });
        });
    }

    public void unregister() {
        AsyncChatEvent.getHandlerList().unregister(this);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
    }
}
