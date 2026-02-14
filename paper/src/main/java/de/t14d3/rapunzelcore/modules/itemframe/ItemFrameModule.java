package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzellib.config.YamlConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Map;

/**
 * Module for item frame management including:
 * - Invisibility toggle
 * - Lock/unlock items and rotation
 * - Glow toggle
 * - Pass-through interactions to containers
 */
public class ItemFrameModule implements Module, Listener {

    private RapunzelPaperCore plugin;
    private boolean enabled = false;
    
    private ItemFrameCommand command;
    private YamlConfig config;
    
    // Cached configuration values - general
    private boolean soundsEnabled;
    private boolean passThroughEnabled;
    private double maxDistance;
    
    // Cached configuration values - invisibility
    private boolean invisibilityEnabled;
    private boolean invisibilityRequireSneak;
    private Material invisibilityTool;
    private String invisibilityPermission;
    
    // Cached configuration values - lock
    private boolean lockEnabled;
    private boolean lockRequireSneak;
    private Material lockTool;
    private String lockPermission;
    
    // Cached configuration values - glow
    private boolean glowEnabled;
    private boolean glowRequireSneak;
    private Material glowTool;
    private String glowPermission;

    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    public String getName() {
        return "itemframe";
    }

    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.itemframe", "op"),
                Map.entry("rapunzelcore.itemframe.invisible", "op"),
                Map.entry("rapunzelcore.itemframe.lock", "op"),
                Map.entry("rapunzelcore.itemframe.unlock", "op"),
                Map.entry("rapunzelcore.itemframe.glow", "op")
        );
    }

    public void enableInternal(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        this.enabled = true;

        if (getEnvironment() != Environment.PAPER) return;

        // Load and cache configuration values
        this.config = loadConfig();
        loadConfiguration();

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register commands
        this.command = new ItemFrameCommand(plugin, this);

    }

    /**
     * Loads and caches configuration values.
     */
    private void loadConfiguration() {
        // General settings
        this.soundsEnabled = config.getBoolean("sounds.enabled", true);
        this.passThroughEnabled = config.getBoolean("pass-through.enabled", true);
        this.maxDistance = config.getDouble("max-distance", 5.0);
        
        // Invisibility settings
        this.invisibilityEnabled = config.getBoolean("invisibility.enabled", true);
        this.invisibilityRequireSneak = config.getBoolean("invisibility.require-sneak", true);
        this.invisibilityPermission = config.getString("invisibility.permission", "rapunzelcore.itemframe.invisible");
        this.invisibilityTool = parseMaterial("invisibility.tool", "SHEARS");
        
        // Lock settings
        this.lockEnabled = config.getBoolean("lock.enabled", true);
        this.lockRequireSneak = config.getBoolean("lock.require-sneak", true);
        this.lockPermission = config.getString("lock.permission", "rapunzelcore.itemframe.lock");
        this.lockTool = parseMaterial("lock.tool", "GLASS_PANE");
        
        // Glow settings
        this.glowEnabled = config.getBoolean("glow.enabled", true);
        this.glowRequireSneak = config.getBoolean("glow.require-sneak", true);
        this.glowPermission = config.getString("glow.permission", "rapunzelcore.itemframe.glow");
        this.glowTool = parseMaterial("glow.tool", "GLOWSTONE");
    }
    
    /**
     * Parses a material from configuration with error handling.
     */
    private Material parseMaterial(String configPath, String defaultMaterial) {
        String materialName = config.getString(configPath, defaultMaterial);
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid " + configPath + " in configuration: " + materialName + ", defaulting to " + defaultMaterial);
            return Material.valueOf(defaultMaterial);
        }
    }


    @Override
    public void enable(RapunzelCore core) {
        if (core instanceof RapunzelPaperCore paperCore) {
            enableInternal(paperCore);
        } else {
            throw new IllegalArgumentException("ItemFrameModule requires RapunzelPaperCore");
        }
    }

    public void disable() {
        this.enabled = false;
        if (getEnvironment() == Environment.PAPER) {
            if (command != null) {
                command.unregister();
                command = null;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame frame)) {
            return;
        }

        Player player = event.getPlayer();
        Material heldItem = player.getInventory().getItemInMainHand().getType();

        // Handle invisibility toggle
        if (invisibilityEnabled && heldItem == invisibilityTool) {
            if (player.hasPermission(invisibilityPermission) &&
                (!invisibilityRequireSneak || player.isSneaking())) {
                
                event.setCancelled(true);
                boolean nowInvisible = toggleInvisible(frame);
                sendFeedback(player, nowInvisible ? "itemframe.invisible.enabled" : "itemframe.invisible.disabled");
                playSound(player, frame, nowInvisible);
                return;
            }
        }

        // Handle lock/unlock toggle
        if (lockEnabled && heldItem == lockTool) {
            if (player.hasPermission(lockPermission) &&
                (!lockRequireSneak || player.isSneaking())) {
                
                event.setCancelled(true);
                boolean nowLocked = toggleLock(frame);
                sendFeedback(player, nowLocked ? "itemframe.lock.enabled" : "itemframe.lock.disabled");
                playSound(player, frame, nowLocked);
                return;
            }
        }

        // Handle glow toggle
        if (glowEnabled && heldItem == glowTool) {
            if (player.hasPermission(glowPermission) &&
                (!glowRequireSneak || player.isSneaking())) {
                
                event.setCancelled(true);
                boolean nowGlowing = toggleGlow(frame);
                sendFeedback(player, nowGlowing ? "itemframe.glow.enabled" : "itemframe.glow.disabled");
                playSound(player, frame, nowGlowing);
                return;
            }
        }

        // Handle pass-through: if NOT sneaking and frame has item, open container behind
        if (!player.isSneaking() && frame.getItem().getType() != Material.AIR) {
            if (passThroughEnabled) {
                Block behind = getBlockBehind(frame);
                if (behind != null && behind.getState() instanceof Container) {
                    event.setCancelled(true);
                    player.openInventory(((Container) behind.getState()).getInventory());
                    return;
                }
            }
        }

        // Check if frame is locked (using fixed property)
        if (frame.isFixed()) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.locked"));
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        // Check if frame is locked (using fixed property)
        if (frame.isFixed()) {
            if (event.getDamager() instanceof Player player) {
                player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.locked"));
            }
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Sends feedback message to player.
     */
    private void sendFeedback(Player player, String messageKey) {
        player.sendMessage(plugin.getMessageHandler().getMessage(messageKey));
    }

    /**
     * Plays sound feedback for actions.
     */
    private void playSound(Player player, ItemFrame frame, boolean enabled) {
        if (soundsEnabled) {
            Sound sound = enabled ? Sound.BLOCK_AMETHYST_BLOCK_CHIME : Sound.BLOCK_AMETHYST_BLOCK_BREAK;
            player.playSound(frame.getLocation(), sound, 0.5f, 1.0f);
        }
    }

    /**
     * Gets the block behind an item frame.
     * @param frame the item frame
     * @return the block behind, or null
     */
    private Block getBlockBehind(ItemFrame frame) {
        Location frameLoc = frame.getLocation();
        return frameLoc.getBlock().getRelative(frame.getAttachedFace());
    }

    /**
     * Toggles visibility of an item frame.
     * @param frame the item frame
     * @return true if now invisible, false if now visible
     */
    public boolean toggleInvisible(ItemFrame frame) {
        boolean isInvisible = !frame.isVisible();
        frame.setVisible(isInvisible);
        return isInvisible;
    }

    /**
     * Toggles lock state of an item frame.
     * @param frame the item frame
     * @return true if now locked, false if now unlocked
     */
    public boolean toggleLock(ItemFrame frame) {
        boolean isLocked = !frame.isFixed();
        frame.setFixed(isLocked);
        return isLocked;
    }

    /**
     * Toggles glow effect of an item frame.
     * @param frame the item frame
     * @return true if now glowing, false if no longer glowing
     */
    public boolean toggleGlow(ItemFrame frame) {
        boolean isGlowing = !frame.isGlowing();
        frame.setGlowing(isGlowing);
        return isGlowing;
    }

    /**
     * Locks an item frame, preventing item removal and rotation.
     * @param frame the item frame
     */
    public void lock(ItemFrame frame) {
        frame.setFixed(true);
    }

    /**
     * Unlocks an item frame.
     * @param frame the item frame
     */
    public void unlock(ItemFrame frame) {
        frame.setFixed(false);
    }

    /**
     * Checks if an item frame is locked.
     * @param frame the item frame
     * @return true if locked
     */
    public boolean isLocked(ItemFrame frame) {
        return frame.isFixed();
    }

    /**
     * Checks if an item frame is invisible.
     * @param frame the item frame
     * @return true if invisible
     */
    public boolean isInvisible(ItemFrame frame) {
        return !frame.isVisible();
    }

    /**
     * Checks if an item frame is glowing.
     * @param frame the item frame
     * @return true if glowing
     */
    public boolean isGlowing(ItemFrame frame) {
        return frame.isGlowing();
    }

    /**
     * Finds the item frame a player is looking at within range.
     * @param player the player
     * @return the item frame, or null
     */
    public ItemFrame getTargetFrame(Player player) {
        Entity target = player.getTargetEntity((int) maxDistance);
        
        if (target instanceof ItemFrame frame) {
            return frame;
        }
        
        // Fallback: check nearby entities
        for (Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof ItemFrame frame) {
                if (isPlayerLookingAt(player, frame)) {
                    return frame;
                }
            }
        }
        
        return null;
    }

    /**
     * Checks if a player is looking at an item frame.
     */
    private boolean isPlayerLookingAt(Player player, ItemFrame frame) {
        Location eyeLoc = player.getEyeLocation();
        Location frameLoc = frame.getLocation();
        
        double distance = eyeLoc.distance(frameLoc);
        if (distance > maxDistance) {
            return false;
        }
        
        return player.hasLineOfSight(frame);
    }

    /**
     * Gets the plugin instance.
     * @return the plugin
     */
    public RapunzelPaperCore getPlugin() {
        return plugin;
    }

    /**
     * Gets the configuration value for pass-through enabled.
     * @return true if pass-through is enabled
     */
    public boolean isPassThroughEnabled() {
        return passThroughEnabled;
    }

    /**
     * Gets the required sneak state for invisibility toggle.
     * @return true if sneaking is required
     */
    public boolean isSneakRequired() {
        return invisibilityRequireSneak;
    }
}
