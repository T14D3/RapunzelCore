package de.t14d3.core;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

public final class Main extends JavaPlugin {
    private MessageHandler messages;
    private static Main instance;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        messages = new MessageHandler(this);
        commandManager = new CommandManager(this);

        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File messagesFile = new File(getDataFolder(), "messages.properties");
        if (!messagesFile.exists()) {
            try {
                //noinspection DataFlowIssue // File is always included in jar
                Files.copy(getClass().getResourceAsStream("/messages.properties"), messagesFile.toPath());
            } catch (Exception e) {
                getLogger().severe("Failed to copy default messages.properties: " + e.getMessage());
            }
        }
    }

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .verboseOutput(false)
                .skipReloadDatapacks(true)
                .silentLogs(true)
        );
        instance = this;
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

    public CommandManager getCommandManager() {
        return commandManager;
    }


}
