package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FlyCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("fly")
                .withAliases("f")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("rapunzelcore.fly.others")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                builder.suggest(player.getName());
                            });
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Toggles flight mode for the given player.")
                .withPermission("rapunzelcore.fly")
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");
                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.fly.error.invalid"));
                        return Command.SINGLE_SUCCESS;
                    }
                    boolean enabled = target.getAllowFlight();
                    target.setAllowFlight(!enabled);
                    if (enabled) {
                        target.setFlying(false);
                    } else {
                        //noinspection deprecation
                        if (target.isOnGround()) {
                            target.setFlying(true);
                        }
                    }
                    Component message = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.fly.toggle",
                            target.getName(), !enabled ? "enabled" : "disabled");
                    sender.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
