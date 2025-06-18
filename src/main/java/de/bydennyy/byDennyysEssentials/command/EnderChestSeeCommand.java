package de.bydennyy.byDennyysEssentials.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class EnderChestSeeCommand {

    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("ec");

        root.requires(sender -> sender.getSender().hasPermission("dennyy.essentials.command.ec.self"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        sender.sendPlainMessage("Only players can use this command!");
                        return Command.SINGLE_SUCCESS;
                    }

                    player.openInventory(player.getEnderChest());

                    return Command.SINGLE_SUCCESS;
                });

        root.then(Commands.argument("target", ArgumentTypes.player())
                .requires(sender -> sender.getSender().hasPermission("dennyy.essentials.command.ec"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        sender.sendPlainMessage("Only players can use this command!");
                        return Command.SINGLE_SUCCESS;
                    }
                    final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                    final Player target = targetResolver.resolve(ctx.getSource()).getFirst();

                    player.openInventory(target.getEnderChest());

                    return Command.SINGLE_SUCCESS;
                }));

        return root.build();

    }
}
