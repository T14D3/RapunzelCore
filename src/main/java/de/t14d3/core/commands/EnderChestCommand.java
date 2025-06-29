package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class EnderChestCommand {
    public static final InventoryHolder ENDER_CHEST_HOLDER = new InventoryHolder() {
        @Override
        public Inventory getInventory() {
            return null;
        }
    };

    public EnderChestCommand() {

        new CommandAPICommand("enderchest")
                .withAliases("ec", "echest")
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("core.enderchest.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withFullDescription("Opens an ender chest for the given player.")
                .withPermission("core.enderchest")
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    // Permission check for viewing others' enderchest
                    if (!sender.equals(target) && !sender.hasPermission("core.enderchest.others")) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.enderchest.error.permission"));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (sender.getOpenInventory().getTopInventory().getType() != InventoryType.PLAYER) {
                        sender.closeInventory();
                    }
                    Inventory enderChest = sender.hasPermission("core.enderchest.edit.others")
                            ? target.getEnderChest()
                            : Bukkit.createInventory(ENDER_CHEST_HOLDER, InventoryType.ENDER_CHEST);
                    if (enderChest.getHolder() == ENDER_CHEST_HOLDER) {
                        enderChest.setContents(target.getEnderChest().getContents());
                    }

                    sender.openInventory(enderChest);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
