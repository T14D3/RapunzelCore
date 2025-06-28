package de.t14d3.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class MessageHandler {
    private final Map<String, String> messages = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageHandler(Main plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.properties");
        Properties messagesConfig = new Properties();
        try {
            messagesConfig.load(new FileInputStream(messagesFile));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load messages.properties: " + e.getMessage());
        }
        messagesConfig.keySet().forEach(key -> {
            messages.put(key.toString(), messagesConfig.getProperty(key.toString()));
        });
    }

    public Component getMessage(String key) {
        if (messages.get(key) == null) {
            return Component.text("No message found for key: " + key);
        }
        return mm.deserialize(messages.get(key));
    }


    public Component getMessage(String key, String arg1) {
        if (messages.get(key) == null) {
            return Component.text("No message found for key: " + key);
        }
        return mm.deserialize(messages.get(key), parsed("arg1", arg1));
    }
    public Component getMessage(String key, String... args) {
        if (messages.get(key) == null) {
            return Component.text("No message found for key: " + key);
        }
        TagResolver[] resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            resolvers[i] = parsed("arg" + (i + 1), args[i]);
        }
        return mm.deserialize(messages.get(key), resolvers);
    }


}
