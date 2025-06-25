package de.t14d3.core;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public final class Main extends JavaPlugin {
    private MessageHandler messages;
    private static Main instance;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        if (Files.notExists(getDataFolder().toPath().resolve("messages.yml"))) {
            try {
                Files.copy(getClass().getResourceAsStream("messages.yml"), getDataFolder().toPath().resolve("messages.yml"));
            } catch (Exception e) {
                getLogger().severe("Failed to copy default messages.yml: " + e.getMessage());
            }
        }


        messages = new MessageHandler(this);
        instance = this;

        new CommandManager(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MessageHandler getMessages() {
        return messages;
    }

    public Component getMessage(String key) {
        return messages.getMessage(key);
    }

    public Component getMessage(String key, String... args) {
        return messages.getMessage(key, args);
    }

    public static Main getInstance() {
        return instance;
    }


}
