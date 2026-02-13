package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class HatCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("hat")
                .withFullDescription("Puts the held item on your head.")
                .withPermission("rapunzelcore.commands.hat")
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.commands.hat.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    PlayerInventory inventory = target.getInventory();
                    ItemStack heldItem = inventory.getItemInMainHand();

                    // Check if player is holding an item
                    if (heldItem.getType() == Material.AIR) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.hat.error.no_item"));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Get current helmet
                    ItemStack currentHelmet = inventory.getHelmet();

                    // Set the held item as helmet
                    inventory.setHelmet(heldItem.clone());
                    
                    // Remove the held item from hand
                    inventory.setItemInMainHand(new ItemStack(Material.AIR));

                    // If there was a helmet, give it back to the player
                    if (currentHelmet != null && currentHelmet.getType() != Material.AIR) {
                        // Try to add to inventory, drop if full
                        if (sender.equals(target)) {
                            sender.getInventory().addItem(currentHelmet).forEach((slot, item) -> {
                                sender.getWorld().dropItemNaturally(sender.getLocation(), item);
                            });
                        } else {
                            target.getInventory().addItem(currentHelmet).forEach((slot, item) -> {
                                target.getWorld().dropItemNaturally(target.getLocation(), item);
                            });
                        }
                    }

                    if (sender.equals(target)) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.hat.success.self", heldItem.getType().name().toLowerCase().replace("_", " ")));
                    } else {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.hat.success.other", target.getName(), heldItem.getType().name().toLowerCase().replace("_", " ")));
                        target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.hat.success.target", sender.getName(), heldItem.getType().name().toLowerCase().replace("_", " ")));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
