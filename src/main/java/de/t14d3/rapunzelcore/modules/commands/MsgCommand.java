package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class MsgCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("msg")
                .withAliases("tell", "whisper", "w")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player"),
                        new StringArgument("message")
                                .withPermission("rapunzelcore.msg")
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = (Player) args.get("player");
                    String message = (String) args.get("message");

                    if (target == null) {
                        sender.sendMessage(Main.getInstance().getMessage("commands.msg.error.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    Component senderMessage = Main.getInstance().getMessage("commands.msg.format.sender",
                                    target.getName(), message)
                            .color(NamedTextColor.GRAY);
                    Component targetMessage = Main.getInstance().getMessage("commands.msg.format.target",
                                    sender.getName(), message)
                            .color(NamedTextColor.GRAY);

                    sender.sendMessage(senderMessage);
                    target.sendMessage(targetMessage);

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sends a private message to the given player.")
                .register(Main.getInstance());
    }
}
