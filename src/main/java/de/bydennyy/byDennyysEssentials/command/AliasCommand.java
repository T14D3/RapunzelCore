package de.bydennyy.byDennyysEssentials.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.bydennyy.byDennyysEssentials.Main;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class AliasCommand {


    public static LiteralCommandNode<CommandSourceStack> createCommand() {
        return Commands.literal("alias")
                .requires(sender -> sender.getSender().hasPermission("dennyy.essentials.command.alias"))
                .executes(ctx -> {
                    if(Main.luckPerms == null){
                        return Command.SINGLE_SUCCESS;
                    }
                    CommandSender sender = ctx.getSource().getSender();

                    if (!(sender instanceof Player player)) {
                        sender.sendPlainMessage("Only players can use this command!");
                        return Command.SINGLE_SUCCESS;
                    }
                    User user = Main.luckPerms.getUserManager().getUser(player.getUniqueId());
                    if(user == null) {
                        player.sendPlainMessage("Unexpected Error!");
                        return Command.SINGLE_SUCCESS;
                    }
                    if (player.hasPermission("dennyy.essentials.alias.show")) {
                        user.data().add(Node.builder("dennyy.essentials.alias.show").value(false).build());
                        sender.sendPlainMessage("Alias command from byDennyysEssential wurden für dich deaktiviert!");
                    } else {
                        user.data().remove(Node.builder("dennyy.essentials.alias.show").value(false).build());
                        sender.sendPlainMessage("Alias command from byDennyysEssential wurden für dich wieder aktiviert!");
                    }

                    return Command.SINGLE_SUCCESS;
                }).build();
    }

}
