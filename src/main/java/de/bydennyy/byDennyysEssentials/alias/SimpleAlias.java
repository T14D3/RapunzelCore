package de.bydennyy.byDennyysEssentials.alias;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;

@SuppressWarnings("UnstableApiUsage")
public class SimpleAlias {
    public static LiteralCommandNode<CommandSourceStack> createCommand(final String alias, final String command, final String permission) {
        return Commands.literal(alias)
                .requires(sender -> sender.getSender().hasPermission(permission) && sender.getSender().hasPermission("dennyy.essentials.alias.show"))
                .executes(ctx -> {
                    Bukkit.dispatchCommand(ctx.getSource().getSender(), command);
                    return Command.SINGLE_SUCCESS;
                }).build();
    }

}
