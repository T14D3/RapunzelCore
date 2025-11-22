package de.t14d3.core.modules.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class HealCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("heal")
                .withFullDescription("Heals the specified player.")
                .withPermission("core.heal")
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("core.heal.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }
                    target.setHealth(target.getAttribute(Attribute.MAX_HEALTH).getValue());
                    sender.sendMessage(Main.getInstance().getMessage("commands.heal.success", target.getName()));
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
