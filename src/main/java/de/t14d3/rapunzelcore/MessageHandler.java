package de.t14d3.rapunzelcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class MessageHandler {
    private final Map<String, String> rawMessages = new HashMap<>();
    private final Map<String, Component> cachedMessages = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageHandler(Main plugin) {
        reloadMessages(plugin);
    }

    public void reloadMessages(Main plugin) {
        rawMessages.clear();
        cachedMessages.clear();
        File messagesFile = new File(plugin.getDataFolder(), "messages.properties");
        Properties messagesConfig = new Properties();
        try {
            messagesConfig.load(new FileInputStream(messagesFile));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load messages.properties: " + e.getMessage());
        }
        messagesConfig.keySet().forEach(key -> {
            String raw = messagesConfig.getProperty(key.toString());
            rawMessages.put(key.toString(), raw);
            cachedMessages.put(key.toString(), mm.deserialize(raw));
        });
    }

    public Component getMessage(String key) {
        Component message = cachedMessages.get(key);
        if (message == null) {
            return Component.text("No message found for key: " + key);
        }
        return message;
    }


    public Component getMessage(String key, String arg1) {
        String raw = rawMessages.get(key);
        if (raw == null) {
            return Component.text("No message found for key: " + key);
        }
        return mm.deserialize(raw, parsed("arg1", arg1));
    }

    public Component getMessage(String key, String... args) {
        String raw = rawMessages.get(key);
        if (raw == null) {
            return Component.text("No message found for key: " + key);
        }
        TagResolver[] resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            resolvers[i] = parsed("arg" + (i + 1), args[i]);
        }
        return mm.deserialize(raw, resolvers);
    }


}
