package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import de.t14d3.core.util.PlayerInventoryMirror;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class InvSeeCommand {
    public InvSeeCommand() {
        new CommandAPICommand("invsee")
                .withAliases("is", "inv")
                .withArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("core.invsee")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Opens the inventory of the given player for inspection and modification.")
                .withPermission("core.invsee")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player)) {
                        executor.sendMessage("§cThis command can only be used by players.");
                        return Command.SINGLE_SUCCESS;
                    }

                    Player sender = (Player) executor; // The player who executed the command
                    Player target = (Player) args.get("player"); // The player whose inventory is to be viewed

                    // Check if the target player is valid (online and exists)
                    if (target == null || !target.isOnline()) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.invsee.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Prevent a player from inspecting their own inventory
                    if (sender.equals(target)) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.invsee.error.self"));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Close any currently open inventory for the sender before opening the mirror
                    // This prevents issues with multiple open inventories
                    sender.closeInventory();

                    // Create and open the mirrored inventory for the viewer
                    PlayerInventoryMirror mirror = new PlayerInventoryMirror(sender, target);
                    mirror.open();

                    sender.sendMessage(Main.getInstance().getMessage("commands.invsee.opened", target.getName()));
                    return Command.SINGLE_SUCCESS; // Command executed successfully
                })
                .register();
    }
}
