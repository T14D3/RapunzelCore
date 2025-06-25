package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public class EnderChestCommand {
    public EnderChestCommand() {
        new CommandAPICommand("enderchest")
                .withAliases("ec", "echest")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.enderchest.others")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                        .executes((executor, args) -> {
                            Player sender = (Player) executor;
                            Player player = (Player) args.get("player");
                            if (player == null) {
                                sender.sendMessage(Main.getInstance().getMessage("commands.enderchest.error.invalid", args.getRaw("player")));
                                return Command.SINGLE_SUCCESS;
                            }
                            if (sender.getOpenInventory().getTopInventory().getType() != InventoryType.PLAYER) {
                                sender.closeInventory();
                            }
                            sender.openInventory(player.getEnderChest());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .withFullDescription("Opens an ender chest for the given player.")
                .withPermission("core.enderchest")
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    if (sender.getOpenInventory().getTopInventory().getType() != InventoryType.PLAYER) {
                        sender.closeInventory();
                    }
                    sender.openInventory(sender.getEnderChest());
                    return Command.SINGLE_SUCCESS;
                });
    }
}
