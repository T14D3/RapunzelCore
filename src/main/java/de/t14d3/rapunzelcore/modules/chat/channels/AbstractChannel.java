package de.t14d3.rapunzelcore.modules.chat.channels;

import de.t14d3.rapunzelcore.util.Messenger;
import de.t14d3.rapunzelcore.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

/**
 * Abstract base class for chat channels providing common functionality.
 */
public abstract class AbstractChannel implements Channel {
    protected final String name;
    protected final String format;
    protected final String shortcut;
    protected final String permission;
    protected final boolean crossServer;
    protected final MiniMessage mm = MiniMessage.miniMessage();
    protected final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    public AbstractChannel(String name, String format, String shortcut, String permission, boolean crossServer) {
        this.name = name;
        this.format = format;
        this.shortcut = shortcut;
        this.permission = permission;
        this.crossServer = crossServer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public String getShortcut() {
        return shortcut;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public boolean isCrossServer() {
        return crossServer;
    }

    @Override
    public void sendMessage(Player sender, Component message) {
        Team team = sender.getScoreboard().getPlayerTeam(sender);
        Component formattedMessage = sender.hasPermission("rapunzelcore.chat.format")
                ? mm.deserialize(plainSerializer.serialize(message), StandardTags.defaults(), Utils.itemResolver(sender))
                : message;
        TagResolver[] resolvers = new TagResolver[]{
                component("message", formattedMessage),
                component("prefix", team == null ? Component.empty() : team.prefix()),
                component("suffix", team == null ? Component.empty() : team.suffix()),
                parsed("channel", name),
                parsed("short", shortcut),
                StandardTags.defaults(),
                component("name", sender.displayName()),
                parsed("player", sender.getName()),
        };
        Component result = mm.deserialize(format, resolvers);
        getReceivers(sender).forEach(receiver -> receiver.sendMessage(result));

        if (crossServer) {
            Messenger.getInstance().sendMessage(sender, result);
        }
    }

    @Override
    public boolean hasPermission(Player sender) {
        if (permission == null || permission.trim().isEmpty()) {
            return true; // No permission required
        }
        return sender.hasPermission(permission);
    }
}
