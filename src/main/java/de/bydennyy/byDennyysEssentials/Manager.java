package de.bydennyy.byDennyysEssentials;

import de.bydennyy.byDennyysEssentials.listener.InventoryListener;
import org.bukkit.plugin.Plugin;

public class Manager {

    private final Plugin plugin;

    public Manager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        registerListener();
    }

    public void registerListener() {
        plugin.getServer().getPluginManager().registerEvents(new InventoryListener(plugin), plugin);
    }

}
