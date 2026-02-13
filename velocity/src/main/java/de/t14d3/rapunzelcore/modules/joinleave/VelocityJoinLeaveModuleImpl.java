package de.t14d3.rapunzelcore.modules.joinleave;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import de.t14d3.rapunzelcore.RapunzelVelocityCore;
import de.t14d3.rapunzelcore.modules.JoinLeaveModule.JoinLeaveModuleImpl;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.common.message.YamlMessageFormatService;
import de.t14d3.rapunzellib.config.ConfigService;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;

import java.nio.file.Path;

public class VelocityJoinLeaveModuleImpl implements JoinLeaveModuleImpl {
    private final RapunzelVelocityCore core;
    private final boolean broadcastEnabled;
    private final Path configPath;

    private MessageFormatService messages;

    public VelocityJoinLeaveModuleImpl(RapunzelVelocityCore core, YamlConfig config, Path configPath) {
        this.core = core;
        this.broadcastEnabled = config.getBoolean("proxy-enabled", true);
        this.configPath = configPath;
    }

    @Override
    public void initialize() {
        ConfigService configService = Rapunzel.context().services().get(ConfigService.class);
        this.messages = new YamlMessageFormatService(configService, core.getLogger(), configPath, "modules/joinleave.yaml");
        core.getServer().getEventManager().register(core, this);
    }

    @Override
    public void cleanup() {
        if (core != null) {
            core.getServer().getEventManager().unregisterListeners(this);
        }
        this.messages = null;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!broadcastEnabled || messages == null) return;

        core.getServer().sendMessage(messages.component("join-message", placeholders(event.getPlayer())));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (!broadcastEnabled || messages == null) return;

        core.getServer().sendMessage(messages.component("leave-message", placeholders(event.getPlayer())));
    }

    private static Placeholders placeholders(Player player) {
        return Placeholders.builder()
                .string("player", player.getUsername())
                .build();
    }
}
