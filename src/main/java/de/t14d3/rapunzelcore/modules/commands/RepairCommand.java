package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RepairCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("repair")
                .withArguments(new StringArgument("type").replaceSuggestions((sender, builder) -> {
                    builder.suggest("hand").suggest("armor").suggest("all");
                    return builder.buildFuture();
                }))
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("rapunzelcore.repair.others")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Repairs the selected items of the target player.")
                .withPermission("rapunzelcore.repair")
                .executes((executor, args) -> {
                    Player target = args.get("player") == null ? (Player) executor : (Player) args.get("player");
                    String type = (String) args.get("type");

                    switch (type) {
                        case "hand":
                            repairItem(target.getInventory().getItemInMainHand());
                            break;
                        case "armor":
                            for (ItemStack item : target.getInventory().getArmorContents()) {
                                repairItem(item);
                            }
                            break;
                        case "all":
                            for (ItemStack item : target.getInventory().getContents()) {
                                repairItem(item);
                            }
                            for (ItemStack item : target.getInventory().getArmorContents()) {
                                repairItem(item);
                            }
                            break;
                    }
                    executor.sendMessage(Main.getInstance().getMessageHandler().getMessage("commands.repair.success", type, target.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }

    private void repairItem(ItemStack item) {
        if (item != null && item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }
}
