package de.t14d3.rapunzelcore;

import de.t14d3.rapunzelcore.commands.CoreCommand;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.modules.Module;
import de.t14d3.rapunzelcore.modules.ModuleManager;
import de.t14d3.rapunzelcore.modules.teleports.WarpsRepository;
import de.t14d3.rapunzelcore.util.ReflectionsUtil;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

public final class Main extends JavaPlugin {
    private MessageHandler messages;
    private static Main instance;
    private CoreDatabase coreDatabase;

    public static final boolean DEBUG = true;

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        messages = new MessageHandler(this);
        coreDatabase = new CoreDatabase(this);
        saveDefaultConfig();


        // Load modules from config
        loadModules();

        new CoreCommand();


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
        CommandAPI.onLoad(
                new CommandAPIPaperConfig((JavaPlugin) this)
                        .verboseOutput(true)
                        .silentLogs(false)
        );
        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public MessageHandler getMessageHandler() {
        return messages;
    }

    public static Main getInstance() {
        return instance;
    }

    public CoreDatabase getCoreDatabase() {
        return coreDatabase;
    }


    public void reloadPlugin() {
        // Disable all modules
        for (Module module : ModuleManager.getModules()) {
            getLogger().info("Disabling module: " + module.getName());
            module.disable(this);
            ModuleManager.disable(module.getName());
        }

        // Reload plugin configuration
        reloadConfig();

        // Reload messages
        messages.reloadMessages(this);

        // Load modules
        loadModules();
    }



    private void loadModules() {
        ReflectionsUtil.getSubTypes(Module.class).forEach(clazz -> {
            try {
                Module module = clazz.getDeclaredConstructor().newInstance();
                String name = module.getName();
                if (getConfig().isBoolean("modules." + name) && getConfig().getBoolean("modules." + name)) {
                    module.enable(this);
                    getLogger().info("Loaded module: " + name);
                }
                ModuleManager.register(module);
            } catch (Exception e) {
                getLogger().warning("Failed to load module " + clazz.getName() + ": " + e.getMessage());
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        });
    }
}
