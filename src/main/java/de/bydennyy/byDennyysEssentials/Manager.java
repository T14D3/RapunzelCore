package de.bydennyy.byDennyysEssentials;

import lombok.Getter;
import org.bukkit.plugin.Plugin;

public class Manager {

    @Getter
    private final Plugin plugin;

    public Manager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        registerListener();
    }

    public void registerListener() {

    }

}
