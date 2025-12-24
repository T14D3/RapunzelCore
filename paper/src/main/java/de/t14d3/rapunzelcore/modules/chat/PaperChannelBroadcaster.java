package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.NetworkEventBus.Subscription;
import de.t14d3.rapunzelcore.util.Utils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.stream.Collectors;

public class PaperChannelBroadcaster {
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final MiniMessage MESSAGE_PLAIN = MiniMessage.builder()
            .strict(false)
            .tags(TagResolver.empty())
            .build();
    private static final MiniMessage MESSAGE_WITH_COLORS = MiniMessage.builder()
            .strict(false)
            .tags(TagResolver.resolver(StandardTags.color(), StandardTags.decorations()))
            .build();

    private final RapunzelPaperCore plugin;
    private final ChannelManager channelManager;
    private final NetworkEventBus eventBus;
    private Subscription incomingSubscription;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public PaperChannelBroadcaster(RapunzelPaperCore plugin, ChannelManager channelManager) {
        this.plugin = plugin;
        this.channelManager = channelManager;
        this.eventBus = new NetworkEventBus(plugin.getMessenger());
    }

    public void broadcastOutgoing(org.bukkit.entity.Player sender, Channel channel, Component message) {
        Component formatted = format(channel, sender, message);
        broadcastLocal(channel, sender, formatted);

        if (channel.isCrossServer()) {
            publishCrossServer(channel.getName(), formatted);
        }
    }

    public void broadcastIncoming(Channel channel, Component formatted) {
        Collection<org.bukkit.entity.Player> receivers = getReceiversForIncoming(channel);
        receivers.forEach(p -> p.sendMessage(formatted));
    }

    private Component format(Channel channel, org.bukkit.entity.Player sender, Component message) {
        String rawFormat = channel.getFormat() != null ? channel.getFormat() : ChatModule.getDefaultFormat();
        MiniMessage messageMiniMessage = sender.hasPermission("rapunzelcore.chat.color")
                ? MESSAGE_WITH_COLORS
                : MESSAGE_PLAIN;
        String rawMessage = PLAIN_SERIALIZER.serialize(message)
                .replace("[i]", "<item>")
                .replace("[item]", "<item>");
        Component formatted = messageMiniMessage.deserialize(rawMessage, Utils.itemResolver(sender));

        TagResolver resolver = TagResolver.resolver(
            Placeholder.parsed("player", sender.getName()),
            Placeholder.component("message", formatted),
            Placeholder.parsed("channel", channel.getName()),
            Placeholder.parsed("short", channel.getShortcut() == null ? "" : channel.getShortcut())
        );
        return miniMessage.deserialize(rawFormat, resolver);
    }

    /**
     * Starts listening for cross-server incoming channel messages.
     * Must be paired with {@link #stopIncomingListener()} to avoid duplicate listeners on /reload.
     */
    public void startIncomingListener() {
        if (incomingSubscription != null) return;

        incomingSubscription = eventBus.register(
            NetworkChannels.CHAT_CHANNEL_MESSAGE,
            ChannelMessagePayload.class,
            (payload, sourceServer) -> {
                if (payload == null) return;
                Channel channel = channelManager.getChannel(payload.getChannelName());
                if (channel == null || !channel.isCrossServer()) return;
                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Component formatted = deserializePayload(payload.getFormattedComponentJson());
                    broadcastIncoming(channel, formatted);
                });
            }
        );
    }

    public void stopIncomingListener() {
        if (incomingSubscription == null) return;
        incomingSubscription.close();
        incomingSubscription = null;
    }

    private void broadcastLocal(Channel channel, org.bukkit.entity.Player sender, Component formatted) {
        Collection<Audience> receivers = getReceiversForOutgoing(channel, sender);
        receivers.add(Bukkit.getServer().getConsoleSender());
        receivers.forEach(p -> p.sendMessage(formatted));
    }

    private Collection<Audience> getReceiversForOutgoing(Channel channel, org.bukkit.entity.Player sender) {
        return switch (channel.getType()) {
            case GLOBAL -> new HashSet<>(Bukkit.getOnlinePlayers());
            case LOCAL -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> Objects.equals(p.getWorld(), sender.getWorld()))
                .filter(p -> p.getLocation().distanceSquared(sender.getLocation()) <= (channel.getRange() * channel.getRange()))
                .collect(Collectors.toSet());
            case PERMISSION -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(channel.getPermission()))
                .collect(Collectors.toSet());
            case PLAYER -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> channelManager.isJoined(Utils.player(p), channel))
                .collect(Collectors.toSet());
        };
    }

    private Collection<org.bukkit.entity.Player> getReceiversForIncoming(Channel channel) {
        // We don't have the sender's location for LOCAL channels; treat incoming cross-server LOCAL as GLOBAL.
        return switch (channel.getType()) {
            case GLOBAL, LOCAL -> new HashSet<>(Bukkit.getOnlinePlayers());
            case PERMISSION -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(channel.getPermission()))
                .collect(Collectors.toSet());
            case PLAYER -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> channelManager.isJoined(Utils.player(p), channel))
                .collect(Collectors.toSet());
        };
    }

    private void publishCrossServer(String channelName, Component formatted) {
        String payloadJson = GSON.serialize(formatted);
        ChannelMessagePayload payload = new ChannelMessagePayload(channelName, payloadJson);

        // Plugin-messaging transport needs an online player to send/forward (PaperPluginMessenger drops when empty); Redis transport does not.
        eventBus.sendToAll(NetworkChannels.CHAT_CHANNEL_MESSAGE, payload);
    }

    public Component deserializePayload(String payloadJson) {
        return GSON.deserialize(payloadJson);
    }
}
