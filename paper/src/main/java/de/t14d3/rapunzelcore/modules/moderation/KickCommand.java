package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class KickCommand implements ModerationCommand {
    private final MuteManager muteManager;

    public KickCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public void register() {
        new CommandAPICommand("kick")
                .withFullDescription("Kicks a player from the server.")
                .withPermission("rapunzelcore.kick")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new GreedyStringArgument("reason")
                )
                .executes((executor, args) -> {
                    Player target = (Player) args.get("player");
                    String reason = args.get("reason") == null ? "No reason provided" : (String) args.get("reason");

                    if (target == null) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player"));
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    // Check if trying to kick someone with bypass permission
                    if (target.hasPermission("rapunzelcore.moderation.bypass")) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.kick.error.bypass", target.getName());
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    // Kick the player
                    Component kickMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.kick.message",
                        reason,
                        executorName
                    );
                    target.kick(kickMsg);

                    // Broadcast kick
                    Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.kick.broadcast",
                        target.getName(),
                        executorName,
                        reason
                    );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.kick.notify");

                    Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.kick.success",
                        target.getName(),
                        reason
                    );
                    executor.sendMessage(successMsg);

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("kick");
    }

    @Override
    public String getName() {
        return "kick";
    }
}
