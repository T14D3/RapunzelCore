package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * Command to lock an item frame.
 */
public class ItemFrameLockCommand {

    private final RapunzelPaperCore plugin;
    private final ItemFrameModule module;

    public ItemFrameLockCommand(RapunzelPaperCore plugin, ItemFrameModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    /**
     * Locks the target item frame.
     * @param player the player executing the command
     */
    public void lock(Player player) {
        ItemFrame frame = module.getTargetFrame(player);
        
        if (frame == null) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.no_target"));
            return;
        }

        // Check if already locked
        if (module.isLocked(frame)) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.lock.already_locked"));
            return;
        }

        // Check if frame has an item (optional - can lock empty frames too)
        if (frame.getItem().getType() == Material.AIR) {
            // Still allow locking empty frames
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.lock.empty_warning"));
        }

        module.lock(frame);
        
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.lock.success"));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);

        // Visual feedback at frame location
        frame.getWorld().spawnParticle(
            Particle.CRIT,
            frame.getLocation().add(0, 0.5, 0),
            30, 0.3, 0.3, 0.3, 0.1
        );
    }
}
