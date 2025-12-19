package de.t14d3.rapunzelcore;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        updateMessages(messagesConfig);
        messagesConfig.keySet().forEach(key -> {
            String raw = messagesConfig.getProperty(key.toString());
            rawMessages.put(key.toString(), raw);
            cachedMessages.put(key.toString(), mm.deserialize(raw));
        });
    }

    private void updateMessages(Properties currentMessages) {
        Properties defaultMessages = new Properties();
        try {
            defaultMessages.load(getClass().getResourceAsStream("/messages.properties"));
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Failed to load default messages.properties: " + e.getMessage());
        }
        AtomicInteger changes = new AtomicInteger();
        defaultMessages.keySet().forEach(key -> {
            String raw = defaultMessages.getProperty(key.toString());
            if (!currentMessages.containsKey(key.toString())) {
                currentMessages.put(key.toString(), raw);
                changes.getAndIncrement();
            }
        });
        Main.getInstance().getLogger().info("Updated " + changes.get() + " messages");
        try {
            currentMessages.store(new FileOutputStream(Main.getInstance().getDataFolder() + "/messages.properties"), "");
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Failed to save messages.properties: " + e.getMessage());
        }

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

    public Component getMessage(String key, Component... args) {
        String raw = rawMessages.get(key);
        if (raw == null) {
            return Component.text("No message found for key: " + key);
        }
        TagResolver[] resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            String placeholder = "arg" + (i + 1);
            resolvers[i] = TagResolver.resolver(placeholder, Tag.inserting(args[i]));
        }
        return mm.deserialize(raw, resolvers);
    }

    public String getRaw(String key) {
        return rawMessages.get(key);
    }
}
