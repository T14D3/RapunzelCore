package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module for item frame management including:
 * - Invisibility toggle
 * - Lock/unlock items and rotation
 * - Pass-through interactions to containers
 */
public class ItemFrameModule implements Module, Listener {

    private RapunzelPaperCore plugin;
    private boolean enabled = false;

    // Track locked frames: entity UUID -> true
    private final Set<UUID> lockedFrames = ConcurrentHashMap.newKeySet();
    
    // Track invisible frames: entity UUID -> true
    private final Set<UUID> invisibleFrames = ConcurrentHashMap.newKeySet();
    
    // Track frames with items: entity UUID -> true (for pass-through logic)
    private final Set<UUID> framesWithItems = ConcurrentHashMap.newKeySet();
    
    private ItemFrameCommand command;

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
                Map.entry("rapunzelcore.itemframe.unlock", "op")
        );
    }

    public void enableInternal(RapunzelPaperCore plugin) {
        this.plugin = plugin;
        this.enabled = true;

        if (getEnvironment() != Environment.PAPER) return;

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register commands
        this.command = new ItemFrameCommand(plugin, this);

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
            lockedFrames.clear();
            invisibleFrames.clear();
            framesWithItems.clear();
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
        UUID frameUuid = frame.getUniqueId();

        // Handle sneak-click toggle for invisibility
        if (plugin.getConfiguration().getBoolean("sneak-click-toggle.enabled", true)) {
            boolean requireSneak = plugin.getConfiguration().getBoolean("sneak-click-toggle.require-sneak", true);
            String toggleItemName = plugin.getConfiguration().getString("sneak-click-toggle.toggle-item", "SHEARS");
            String permission = plugin.getConfiguration().getString("sneak-click-toggle.permission", "rapunzelcore.itemframe.invisible");

            try {
                Material toggleItem = Material.valueOf(toggleItemName.toUpperCase());

                if (player.hasPermission(permission) &&
                    (!requireSneak || player.isSneaking()) &&
                    player.getInventory().getItemInMainHand().getType() == toggleItem) {

                    event.setCancelled(true);
                    boolean nowInvisible = toggleInvisible(frame);

                    // Send feedback message
                    if (nowInvisible) {
                        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.invisible.enabled"));
                    } else {
                        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.invisible.disabled"));
                    }

                    // Play sound if enabled
                    if (plugin.getConfiguration().getBoolean("visual-feedback.sounds", true)) {
                        Sound sound = nowInvisible ? Sound.BLOCK_AMETHYST_BLOCK_CHIME : Sound.BLOCK_AMETHYST_BLOCK_BREAK;
                        player.playSound(frame.getLocation(), sound, 0.5f, 1.0f);
                    }

                    // Show particles if enabled
                    if (plugin.getConfiguration().getBoolean("visual-feedback.particles", true)) {
                        Particle particle = nowInvisible ? Particle.END_ROD : Particle.CRIT;
                        frame.getWorld().spawnParticle(particle, frame.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);
                    }

                    return;
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid toggle-item in configuration: " + toggleItemName);
            }
        }

        // Handle pass-through: if sneaking and frame has item, open container behind
        if (player.isSneaking() && frame.getItem().getType() != Material.AIR) {
            if (plugin.getConfiguration().getBoolean("pass-through.enabled", true)) {
                Block behind = getBlockBehind(frame);
                if (behind != null && behind.getState() instanceof Container) {
                    event.setCancelled(true);
                    // Open the container
                    player.openInventory(((Container) behind.getState()).getInventory());
                    return;
                }
            }
        }

        // Check if frame is locked
        if (lockedFrames.contains(frameUuid)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.locked"));
            return;
        }

        // Update tracking
        if (frame.getItem().getType() != Material.AIR) {
            framesWithItems.add(frameUuid);
        } else {
            framesWithItems.remove(frameUuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) {
            return;
        }

        // Check if frame is locked
        if (lockedFrames.contains(frame.getUniqueId())) {
            if (event.getDamager() instanceof Player player) {
                player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.locked"));
            }
            event.setCancelled(true);
            return;
        }
    }

    /**
     * Gets the block behind an item frame.
     * @param frame the item frame
     * @return the block behind, or null
     */
    private Block getBlockBehind(ItemFrame frame) {
        Location frameLoc = frame.getLocation();
        Block attached = frame.getAttachedFace() != null ? frameLoc.getBlock().getRelative(frame.getAttachedFace()) : null;
        return attached;
    }

    /**
     * Toggles visibility of an item frame.
     * @param frame the item frame
     * @return true if now invisible, false if now visible
     */
    public boolean toggleInvisible(ItemFrame frame) {
        UUID uuid = frame.getUniqueId();
        boolean isInvisible = invisibleFrames.contains(uuid);
        
        if (isInvisible) {
            invisibleFrames.remove(uuid);
            frame.setVisible(true);
            return false;
        } else {
            invisibleFrames.add(uuid);
            frame.setVisible(false);
            return true;
        }
    }

    /**
     * Locks an item frame, preventing item removal and rotation.
     * @param frame the item frame
     */
    public void lock(ItemFrame frame) {
        lockedFrames.add(frame.getUniqueId());
        frame.setFixed(true);
        if (frame.getItem().getType() != Material.AIR) {
            framesWithItems.add(frame.getUniqueId());
        }
    }

    /**
     * Unlocks an item frame.
     * @param frame the item frame
     */
    public void unlock(ItemFrame frame) {
        lockedFrames.remove(frame.getUniqueId());
        frame.setFixed(false);
    }

    /**
     * Checks if an item frame is locked.
     * @param frame the item frame
     * @return true if locked
     */
    public boolean isLocked(ItemFrame frame) {
        return lockedFrames.contains(frame.getUniqueId());
    }

    /**
     * Checks if an item frame is invisible.
     * @param frame the item frame
     * @return true if invisible
     */
    public boolean isInvisible(ItemFrame frame) {
        return invisibleFrames.contains(frame.getUniqueId());
    }

    /**
     * Finds the item frame a player is looking at within range.
     * @param player the player
     * @return the item frame, or null
     */
    public ItemFrame getTargetFrame(Player player) {
        double maxDistance = plugin.getConfiguration().getDouble("max-distance", 5.0);
        Entity target = player.getTargetEntity((int) maxDistance);
        
        if (target instanceof ItemFrame frame) {
            return frame;
        }
        
        // Fallback: check nearby entities
        for (Entity entity : player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
            if (entity instanceof ItemFrame frame) {
                // Check if player is looking at this frame
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
        
        // Simple distance check
        double distance = eyeLoc.distance(frameLoc);
        if (distance > plugin.getConfiguration().getDouble("max-distance", 5.0)) {
            return false;
        }
        
        // Check if in line of sight
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
        return plugin.getConfiguration().getBoolean("pass-through.enabled", true);
    }

    /**
     * Gets the required sneak state for pass-through.
     * @return true if sneaking is required
     */
    public boolean isSneakRequired() {
        return plugin.getConfiguration().getBoolean("pass-through.require-sneak", true);
    }
}
