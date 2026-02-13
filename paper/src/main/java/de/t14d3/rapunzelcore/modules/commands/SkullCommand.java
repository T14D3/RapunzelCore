package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class SkullCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("skull")
                .withFullDescription("Gives you a player skull.")
                .withPermission("rapunzelcore.commands.skull")
                .withOptionalArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("target")
                                .withPermission("rapunzelcore.commands.skull.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    String skullOwner = args.get("player") == null ? sender.getName() : (String) args.get("player");
                    Player target = args.get("target") == null ? sender : (Player) args.get("target");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }

                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(Bukkit.getOfflinePlayer(skullOwner));
                        skull.setItemMeta(meta);
                    }

                    target.getInventory().addItem(skull);

                    if (sender.equals(target)) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.skull.success.self", skullOwner));
                    } else {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.skull.success.other", skullOwner, target.getName()));
                        target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.skull.success.target", sender.getName(), skullOwner));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
