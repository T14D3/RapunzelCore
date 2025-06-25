package de.t14d3.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class MessageHandler {
    private final Main plugin;
    private Map<String, String> messages = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MessageHandler(Main plugin) {
        this.plugin = plugin;
        FileConfiguration messagesConfig = new YamlConfiguration();
        try {
            messagesConfig.load(plugin.getDataFolder().toPath().resolve("messages.yml").toFile());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load messages.yml");
        }
        messagesConfig.getKeys(false).forEach(key -> {
            messages.put(key, messagesConfig.getString(key));
        });
    }

    public Component getMessage(String key) {
        return mm.deserialize(messages.get(key));
    }


    public Component getMessage(String key, String arg1) {
        return mm.deserialize(messages.get(key), parsed("arg1", arg1));
    }
    public Component getMessage(String key, String... args) {
        TagResolver[] resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            resolvers[i] = parsed("arg" + (i + 1), args[i]);
        }
        return mm.deserialize(messages.get(key), resolvers);
    }


}
