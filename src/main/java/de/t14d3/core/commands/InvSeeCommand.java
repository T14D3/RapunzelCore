package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class InvSeeCommand {
    public InvSeeCommand() {
        new CommandAPICommand("invsee")
                .withAliases("is")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.invsee")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                        .executes((executor, args) -> {
                            Player sender = (Player) executor;
                            Player target = (Player) args.get("player");
                            if (target == null) {
                                sender.sendMessage(Main.getInstance().getMessage("commands.invsee.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (sender.getOpenInventory().getTopInventory().getType() != InventoryType.PLAYER) {
                                sender.closeInventory();
                            }
                            sender.openInventory(target.getInventory());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Opens the inventory of the given player.")
                .withPermission("core.invsee")
                .register(Main.getInstance());
    }
}
