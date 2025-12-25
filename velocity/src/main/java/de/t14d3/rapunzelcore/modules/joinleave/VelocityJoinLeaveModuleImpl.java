package de.t14d3.rapunzelcore.modules.joinleave;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzelcore.RapunzelVelocityCore;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule.JoinLeaveModuleImpl;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import net.kyori.adventure.text.Component;

import java.nio.file.Path;
import java.time.Duration;

public class VelocityJoinLeaveModuleImpl implements JoinLeaveModuleImpl {
    private final RapunzelVelocityCore core;
    private final boolean networkEnabled;
    private final Path configPath;

    private MessageFormatService messages;
    private NetworkEventBus bus;

    public VelocityJoinLeaveModuleImpl(RapunzelVelocityCore core, boolean networkEnabled, Path configPath) {
        this.core = core;
        this.networkEnabled = networkEnabled;
        this.configPath = configPath;
    }

    @Override
    public void initialize() {
        ConfigService configService = Rapunzel.context().services().get(ConfigService.class);
        this.messages = new YamlMessageFormatService(configService, core.getLogger(), configPath, "modules/joinleave.yaml");
        this.bus = new NetworkEventBus(core.getMessenger());

        announceProxyHandles();
        core.getServer().getEventManager().register(core, this);
    }

    @Override
    public void cleanup() {
        if (core != null) {
            core.getServer().getEventManager().unregisterListeners(this);
        }
        this.bus = null;
        this.messages = null;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        announceProxyHandles();
        if (!networkEnabled || messages == null) return;

        Component message = messages.component("join-message", placeholders(event.getPlayer()));
        broadcast(message);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        announceProxyHandles();
        if (!networkEnabled || messages == null) return;

        Component message = messages.component("leave-message", placeholders(event.getPlayer()));
        broadcast(message);
    }

    private void broadcast(Component message) {
        ProxyServer server = core.getServer();
        server.getAllPlayers().forEach(p -> p.sendMessage(message));
    }

    private void announceProxyHandles() {
        core.getLogger().info("Should announce proxy handles: " + networkEnabled);
        core.getLogger().info("Bus is null: " + (bus == null));
        if (!networkEnabled || bus == null) return;
        core.getLogger().info("Sending proxy handles");
        bus.sendToAll(
            NetworkChannels.JOIN_LEAVE_BROADCAST,
            new JoinLeaveModule.JoinLeavePayload(true)
        );
        // Just in case, send again after a short delay.
        Rapunzel.context().scheduler().runLater(
            Duration.ofSeconds(1),
            () -> {
                if (!networkEnabled || bus == null) return;
                bus.sendToAll(
                    NetworkChannels.JOIN_LEAVE_BROADCAST,
                    new JoinLeaveModule.JoinLeavePayload(true)
                );
            }
        );
    }

    private static Placeholders placeholders(Player player) {
        return Placeholders.builder()
                .string("player", player.getUsername())
                .build();
    }
}
