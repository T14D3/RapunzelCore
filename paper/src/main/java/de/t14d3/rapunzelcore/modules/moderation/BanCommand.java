package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Date;

public class BanCommand implements ModerationCommand {
    private final MuteManager muteManager;

    public BanCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public void register() {
        new CommandAPICommand("ban")
                .withFullDescription("Bans a player from the server.")
                .withPermission("rapunzelcore.ban")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new TimeArgument("duration")
                                .withPermission("rapunzelcore.ban.temp")
                )
                .withOptionalArguments(
                        new GreedyStringArgument("reason")
                )
                .executes((executor, args) -> {
                    String playerName = (String) args.get("player");
                    Duration duration = args.get("duration") == null ? null : (Duration) args.get("duration");
                    String reason = args.get("reason") == null ? "No reason provided" : (String) args.get("reason");

                    // Look up the player
                    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
                    if (target == null) {
                        // Try to get by name (may return a player even if not cached)
                        target = Bukkit.getOfflinePlayer(playerName);
                    }

                    if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", playerName);
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    // Check if trying to ban someone with bypass permission
                    if (target.isOnline() && target.getPlayer() != null) {
                        Player onlineTarget = target.getPlayer();
                        if (onlineTarget.hasPermission("rapunzelcore.moderation.bypass")) {
                            Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ban.error.bypass", target.getName());
                            executor.sendMessage(msg);
                            return SINGLE_SUCCESS;
                        }
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    if (duration == null) {
                        // Permanent ban
                        if (!executor.hasPermission("rapunzelcore.ban.permanent")) {
                            Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ban.error.no_permanent");
                            executor.sendMessage(msg);
                            return SINGLE_SUCCESS;
                        }

                        // Use modern Bukkit ban API
                        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, null, executorName);

                        // Kick if online
                        if (target.isOnline() && target.getPlayer() != null) {
                            target.getPlayer().kick(Component.text("You are banned: " + reason));
                        }

                        // Broadcast ban
                        Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.ban.broadcast.permanent",
                            target.getName(),
                            executorName,
                            reason
                        );
                        Bukkit.broadcast(broadcastMsg, "rapunzelcore.ban.notify");

                        Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.ban.success.permanent",
                            target.getName(),
                            reason
                        );
                        executor.sendMessage(successMsg);
                    } else {
                        // Temporary ban
                        long expiration = System.currentTimeMillis() + duration.toMillis();
                        Date expirationDate = new Date(expiration);
                        
                        // Use modern Bukkit ban API
                        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, expirationDate, executorName);

                        // Kick if online
                        if (target.isOnline() && target.getPlayer() != null) {
                            target.getPlayer().kick(Component.text("You are temporarily banned: " + reason + " (Expires: " + formatDuration(duration) + ")"));
                        }

                        String durationStr = formatDuration(duration);

                        // Broadcast ban
                        Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.ban.broadcast.temporary",
                            target.getName(),
                            executorName,
                            durationStr,
                            reason
                        );
                        Bukkit.broadcast(broadcastMsg, "rapunzelcore.ban.notify");

                        Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.ban.success.temporary",
                            target.getName(),
                            durationStr,
                            reason
                        );
                        executor.sendMessage(successMsg);
                    }

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Unban command
        new CommandAPICommand("unban")
                .withFullDescription("Unbans a player from the server.")
                .withPermission("rapunzelcore.unban")
                .withAliases("pardon")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    for (OfflinePlayer p : Bukkit.getBannedPlayers()) {
                                        if (p.getName() != null) {
                                            builder.suggest(p.getName());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    String playerName = (String) args.get("player");
                    
                    // Check if player is banned
                    if (!Bukkit.getBanList(BanList.Type.NAME).isBanned(playerName)) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unban.error.not_banned", playerName);
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    // Use modern Bukkit pardon API
                    Bukkit.getBanList(BanList.Type.NAME).pardon(playerName);

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.unban.broadcast",
                        playerName,
                        executorName
                    );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.ban.notify");

                    Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                        "commands.unban.success",
                        playerName
                    );
                    executor.sendMessage(successMsg);

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

        @Override
    public void unregister() {
        CommandAPI.unregister("ban");
        CommandAPI.unregister("unban");
    }

    @Override
    public String getName() {
        return "ban";
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
