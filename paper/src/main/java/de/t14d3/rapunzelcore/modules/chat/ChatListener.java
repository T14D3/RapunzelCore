package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.util.Utils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatListener implements Listener {
    private final RapunzelPaperCore core;
    private final ChannelManager channelManager;
    private final PaperChannelBroadcaster broadcaster;
    private final List<String> commands = List.of(
            "msg",
            "tell",
            "w",
            "r",
            "reply",
            "whisper"
    );

    public ChatListener(RapunzelPaperCore core, ChannelManager channelManager, PaperChannelBroadcaster broadcaster) {
        this.core = core;
        this.channelManager = channelManager;
        this.broadcaster = broadcaster;

        core.getServer().getPluginManager().registerEvents(this, core);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        Component message = event.originalMessage();
        Bukkit.getScheduler().runTask(core, () -> {
            Channel channel = channelManager.getMainChannel(Utils.player(player));
            if (channel == null) {
                return;
            }
            broadcaster.broadcastOutgoing(player, channel, message);
        });
        event.viewers().clear();
    }

    @EventHandler
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().hasPermission("rapunzelcore.socialspy.bypass")) {
            return;
        }
        String[] split = event.getMessage().split(" ", 3);
        if (split.length < 3) return;

        String rawCommand = split[0].startsWith("/") ? split[0].substring(1) : split[0];
        boolean isPrivateMessage = commands.stream().anyMatch(cmd -> cmd.equalsIgnoreCase(rawCommand));
        if (!isPrivateMessage) return;

        String senderName = event.getPlayer().getName();
        String receiverName = split[1];
        String message = split[2];
        Component socialSpyMessage = core.getMessageHandler()
            .getMessage("commands.socialspy.message", senderName, receiverName, message);

        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            List<org.bukkit.entity.Player> spies = new ArrayList<>();
            for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(event.getPlayer().getUniqueId())) continue;
                if (online.hasPermission("rapunzelcore.socialspy.bypass")) continue;
                if (!Utils.player(online).isSocialSpyEnabled()) continue;
                spies.add(online);
            }
            if (spies.isEmpty()) return;
            Bukkit.getScheduler().runTask(core, () -> spies.forEach(p -> p.sendMessage(socialSpyMessage)));
        });
    }

    public void unregister() {
        AsyncChatEvent.getHandlerList().unregister(this);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(this);
    }
}
