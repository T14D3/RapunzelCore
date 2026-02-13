package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.UUID;

public class MuteCommand implements ModerationCommand {
    private final MuteManager muteManager;

    public MuteCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public void register() {
        new CommandAPICommand("mute")
                .withFullDescription("Mutes a player from chatting.")
                .withPermission("rapunzelcore.mute")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new TimeArgument("duration")
                                .withPermission("rapunzelcore.mute.temp")
                )
                .withOptionalArguments(
                        new GreedyStringArgument("reason")
                )
                .executes((executor, args) -> {
                    OfflinePlayer target = Bukkit.getOfflinePlayer((String) args.get("player"));
                    Duration duration = args.get("duration") == null ? null : (Duration) args.get("duration");
                    String reason = args.get("reason") == null ? "No reason provided" : (String) args.get("reason");

                    if (target == null) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player"));
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    UUID executorUuid = executor instanceof Player ? ((Player) executor).getUniqueId() : null;
                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    // Check if player is already muted
                    if (muteManager.isMuted(target.getUniqueId())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.error.already_muted", target.getName());
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    long durationMs = duration == null ? -1 : duration.toMillis();

                    // Check permanent permission
                    if (durationMs < 0 && !executor.hasPermission("rapunzelcore.mute.permanent")) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.error.no_permanent");
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    String serverName = RapunzelPaperCore.getServerName();
                    muteManager.mutePlayer(target.getUniqueId(), executorUuid, reason, durationMs, serverName);

                    String durationStr = durationMs < 0 ? "permanent" : formatDuration(duration);

                    // Broadcast mute
                    Component broadcastMsg = durationMs < 0
                        ? RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.mute.broadcast.permanent",
                            target.getName(),
                            executorName,
                            reason
                        )
                        : RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.mute.broadcast.temporary",
                            target.getName(),
                            executorName,
                            durationStr,
                            reason
                        );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.mute.notify");

                    // Notify executor
                    Component executorMsg = durationMs < 0
                        ? RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.success.permanent", target.getName(), reason)
                        : RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.success.temporary", target.getName(), durationStr, reason);
                    executor.sendMessage(executorMsg);

                    // Notify target if online
                    if (target.isOnline() && target.getPlayer() != null) {
                        Component targetMsg = durationMs < 0
                            ? RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.notify.permanent", executorName, reason)
                            : RapunzelCore.getInstance().getMessageHandler().getMessage("commands.mute.notify.temporary", executorName, durationStr, reason);
                        target.getPlayer().sendMessage(targetMsg);
                    }

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Unmute command
        new CommandAPICommand("unmute")
                .withFullDescription("Unmutes a player.")
                .withPermission("rapunzelcore.unmute")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    OfflinePlayer target = Bukkit.getOfflinePlayer((String) args.get("player"));

                    if (target == null) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player"));
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    if (!muteManager.isMuted(target.getUniqueId())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unmute.error.not_muted", target.getName());
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    muteManager.unmutePlayer(target.getUniqueId());

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    // Broadcast unmute
                    Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.unmute.broadcast",
                        target.getName(),
                        executorName
                    );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.mute.notify");

                    Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unmute.success", target.getName());
                    executor.sendMessage(successMsg);

                    // Notify target if online
                    if (target.isOnline() && target.getPlayer() != null) {
                        Component notifyMsg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unmute.notify", executorName);
                        target.getPlayer().sendMessage(notifyMsg);
                    }

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

        @Override
    public void unregister() {
        CommandAPI.unregister("mute");
        CommandAPI.unregister("unmute");
    }

    @Override
    public String getName() {
        return "mute";
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");

        return sb.toString().trim();
    }
}
