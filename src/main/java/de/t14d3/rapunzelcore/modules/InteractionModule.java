package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Main;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class InteractionModule implements Module {
    private boolean enabled = false;
    private Main plugin;
    private final Map<String, String> interactions = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;

        // Load config
        config = loadConfig(plugin);
        loadInteractions();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(new InteractionListener(this), plugin);
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        enabled = false;
        saveConfig(plugin, config);
    }

    @Override
    public String getName() {
        return "interaction";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setInteraction(Location location, String command) {
        String key = serializeLocation(location);
        if (command == null || command.trim().isEmpty()) {
            interactions.remove(key);
            config.set(key, null);
        } else {
            interactions.put(key, command.trim());
            config.set(key, command.trim());
        }
    }

    public String getInteraction(Location location) {
        return interactions.get(serializeLocation(location));
    }

    public void removeInteraction(Location location) {
        setInteraction(location, null);
    }

    private void loadInteractions() {
        interactions.clear();
        for (String key : config.getKeys(false)) {
            String command = config.getString(key);
            if (command != null && !command.trim().isEmpty()) {
                interactions.put(key, command.trim());
            }
        }
    }

    private String serializeLocation(Location location) {
        return location.getWorld().getName() + "|" + location.getBlockX() + "|" + location.getBlockY() + "|" + location.getBlockZ();
    }

    private static class InteractionListener implements Listener {
        private final InteractionModule module;

        public InteractionListener(InteractionModule module) {
            this.module = module;
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null) return;

            String command = module.getInteraction(event.getClickedBlock().getLocation());
            if (command != null) {
                module.plugin.getServer().dispatchCommand(event.getPlayer(), command);
                event.setCancelled(true);
            }
        }
    }
}

