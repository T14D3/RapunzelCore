package de.t14d3.rapunzelcore.modules.joinleave;

import de.t14d3.rapunzelcore.modules.JoinLeaveModule;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule.JoinLeaveModuleImpl;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.NetworkEventBus.Subscription;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.nio.file.Path;

public class PaperJoinLeaveModuleImpl implements JoinLeaveModuleImpl, Listener {
    private final RapunzelPaperCore plugin;
    private final boolean networkEnabled;
    private final Path configPath;
    private static final long JOIN_BROADCAST_DELAY_TICKS = 40L;

    private MessageFormatService messages;
    private NetworkEventBus bus;
    private Subscription proxySignalSub;
    private boolean proxyHandlesBroadcasts;

    public PaperJoinLeaveModuleImpl(RapunzelPaperCore plugin, boolean networkEnabled, Path configPath) {
        this.plugin = plugin;
        this.networkEnabled = networkEnabled;
        this.configPath = configPath;
    }

    @Override
    public void initialize() {
        ConfigService configService = Rapunzel.context().services().get(ConfigService.class);
        this.messages = new YamlMessageFormatService(configService, plugin.getSLF4JLogger(), configPath, "modules/joinleave.yaml");

        if (networkEnabled) {
            this.bus = new NetworkEventBus(plugin.getMessenger());
            this.proxySignalSub = bus.register(
                NetworkChannels.JOIN_LEAVE_BROADCAST,
                JoinLeaveModule.JoinLeavePayload.class,
                (payload, sourceServer) -> {
                    if (payload != null && payload.proxyHandlesBroadcasts()) {
                        proxyHandlesBroadcasts = true;
                    }
                }
            );
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void cleanup() {
        HandlerList.unregisterAll(this);
        if (proxySignalSub != null) {
            proxySignalSub.close();
            proxySignalSub = null;
        }
        bus = null;
        messages = null;
        proxyHandlesBroadcasts = false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        if (!networkEnabled) {
            broadcastJoin(event.getPlayer().getName());
            return;
        }
        if (shouldSkipLocal()) return;
        String playerName = event.getPlayer().getName();
        // Delay to give the proxy time to announce it will handle broadcasts after restarts.
        plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> {
                if (shouldSkipLocal()) return;
                broadcastJoin(playerName);
            },
            JOIN_BROADCAST_DELAY_TICKS
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        if (shouldSkipLocal()) return;
        broadcastLeave(event);
    }

    private void broadcastJoin(String playerName) {
        Component message = messages.component("join-message", placeholders(playerName));
        plugin.getServer().broadcast(message);
    }

    private void broadcastLeave(PlayerQuitEvent event) {
        Component message = messages.component("leave-message", placeholders(event.getPlayer().getName()));
        plugin.getServer().broadcast(message);
    }

    private boolean shouldSkipLocal() {
        return networkEnabled && proxyHandlesBroadcasts;
    }

    private static Placeholders placeholders(String playerName) {
        return Placeholders.builder()
                .string("player", playerName)
                .build();
    }
}
