package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class NickCommand implements Command {

    @Override
    public void register() {
        RapunzelPaperCore core = (RapunzelPaperCore) RapunzelCore.getInstance();
        new CommandAPICommand("nick")
                .withArguments(new EntitySelectorArgument.OnePlayer("target")
                        .replaceSuggestions((sender, builder) -> {
                            core.getServer().getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                )
                .withArguments(new StringArgument("nickname").replaceSuggestions((sender, builder) -> {
                    builder.suggest("off").suggest("reset");
                    return builder.buildFuture();
                }))
                .withFullDescription("Sets the nickname of the target player.")
                .withPermission("rapunzelcore.nick")
                .executes((executor, args) -> {
                    Player target = (Player) args.get("target");
                    Player sender = (Player) executor;

                    if (target == null) {
                        sender.sendMessage(core.getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("target")));
                        return Command.SINGLE_SUCCESS;
                    }
                    String nickname = (String) args.get("nickname");

                    if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")) {
                        target.displayName(target.name());
                        sender.sendMessage(core.getMessageHandler().getMessage("commands.nick.reset", target.getName()));
                    } else {
                        target.displayName(Component.text(nickname));
                        sender.sendMessage(core.getMessageHandler().getMessage("commands.nick.set", target.getName(), nickname));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register(core);
    }
}
