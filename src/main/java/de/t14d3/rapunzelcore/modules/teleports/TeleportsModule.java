package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.modules.Module;
import org.bukkit.configuration.file.FileConfiguration;

public class TeleportsModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private CoreDatabase coreDatabase;
    private FileConfiguration config;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        this.coreDatabase = plugin.getCoreDatabase();
        this.config = loadConfig();
        enabled = true;

        registerCommands();
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        // Commands are automatically unregistered
        enabled = false;
    }

    @Override
    public String getName() {
        return "teleports";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private final WarpCommands warpCommands;
    private final TpaCommands tpaCommands;
    private final HomeCommands homeCommands;

    public TeleportsModule() {
        Main main = Main.getInstance();
        this.warpCommands = new WarpCommands(main);
        this.tpaCommands = new TpaCommands(main);
        this.homeCommands = new HomeCommands(main);
    }

    private void registerCommands() {
        // Register all command handlers
        warpCommands.register();
        tpaCommands.register();
        homeCommands.register();
    }
}
