package de.bydennyy.byDennyysEssentials.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class LightningCommand {

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("lightning")
                .requires(sender -> sender.getSender().hasPermission("dennyy.essentials.command.lightning"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        sender.sendPlainMessage("Only players can use this command!");
                        return Command.SINGLE_SUCCESS;
                    }
                    World world = player.getWorld();
                    Block block = player.getTargetBlockExact(100);
                    if (block == null) {
                        sender.sendPlainMessage("Block is to far away!");
                        return Command.SINGLE_SUCCESS;

                    }

                    world.spawnEntity(block.getLocation().add(0, 1, 0), EntityType.LIGHTNING_BOLT);

                    return Command.SINGLE_SUCCESS;
                }).build();
    }
}
