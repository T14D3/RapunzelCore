package de.t14d3.core.modules.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class GodCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("god")
                .withFullDescription("Toggles god mode for the specified player.")
                .withPermission("core.god")
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("core.god.others")
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
                    target.setInvulnerable(!target.isInvulnerable());
                    sender.sendMessage(Main.getInstance().getMessage("commands.god.success", target.getName(), target.isInvulnerable() ? "enabled" : "disabled"));
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
