package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.UUID;

public class ModerationListener implements Listener {
    private final MuteManager muteManager;

    public ModerationListener(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        muteManager.loadPlayerData(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        muteManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (muteManager.isMuted(player.getUniqueId())) {
            MuteManager.MuteData muteData = muteManager.getMuteInfo(player.getUniqueId());

            // Cancel the chat event
            event.setCancelled(true);

            // Send mute message to player
            Component message;
            if (muteData != null && muteData.isPermanent()) {
                message = RapunzelCore.getInstance().getMessageHandler().getMessage(
                    "commands.mute.error.chat_permanent",
                    muteData.getReason()
                );
            } else if (muteData != null) {
                long remaining = muteData.getExpiresAt() - System.currentTimeMillis();
                String duration = formatDuration(remaining);
                message = RapunzelCore.getInstance().getMessageHandler().getMessage(
                    "commands.mute.error.chat_temporary",
                    duration,
                    muteData.getReason()
                );
            } else {
                message = RapunzelCore.getInstance().getMessageHandler().getMessage(
                    "commands.mute.error.chat"
                );
            }

            player.sendMessage(message);
        }
    }

        public void unregister() {
        HandlerList.unregisterAll(this);
    }

    private String formatDuration(long millis) {
        if (millis < 0) return "permanent";

        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
