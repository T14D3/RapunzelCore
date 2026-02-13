package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * Command to toggle item frame visibility.
 */
public class ItemFrameInvisibleCommand {

    private final RapunzelPaperCore plugin;
    private final ItemFrameModule module;

    public ItemFrameInvisibleCommand(RapunzelPaperCore plugin, ItemFrameModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    /**
     * Toggles visibility of the target item frame.
     * @param player the player executing the command
     */
    public void toggle(Player player) {
        ItemFrame frame = module.getTargetFrame(player);
        
        if (frame == null) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.error.no_target"));
            return;
        }

        boolean isInvisible = module.toggleInvisible(frame);
        
        if (isInvisible) {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.invisible.enabled"));
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        } else {
            player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.invisible.disabled"));
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
        }

        // Visual feedback at frame location
        frame.getWorld().spawnParticle(
            Particle.WITCH,
            frame.getLocation().add(0, 0.5, 0),
            20, 0.3, 0.3, 0.3, 0.1
        );
    }
}
