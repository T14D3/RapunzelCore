package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * Command to unlock an item frame.
 */
public class ItemFrameUnlockCommand {

    private final RapunzelPaperCore plugin;
    private final ItemFrameModule module;

    public ItemFrameUnlockCommand(RapunzelPaperCore plugin, ItemFrameModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    /**
     * Unlocks the target item frame.
     * @param player the player executing the command
     */
    public void unlock(Player player) {
        ItemFrame frame = module.getTargetFrame(player);
        
        if (frame == null) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.no_target"));
            return;
        }

        // Check if already unlocked
        if (!module.isLocked(frame)) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.unlock.already_unlocked"));
            return;
        }

        module.unlock(frame);
        
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.unlock.success"));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

        // Visual feedback at frame location
        frame.getWorld().spawnParticle(
            Particle.WAX_ON,
            frame.getLocation().add(0, 0.5, 0),
            30, 0.3, 0.3, 0.3, 0.1
        );
    }
}
