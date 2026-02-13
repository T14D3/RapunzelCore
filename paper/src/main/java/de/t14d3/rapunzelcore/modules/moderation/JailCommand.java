package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JailCommand implements ModerationCommand, Listener {
    private final MuteManager muteManager;
    private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();
    private Location jailLocation = null;

    public JailCommand(MuteManager muteManager) {
        this.muteManager = muteManager;
    }

    @Override
    public void register() {
        // Register listener
        Bukkit.getPluginManager().registerEvents(this, (JavaPlugin) RapunzelCore.getInstance());

        // Jail command
        new CommandAPICommand("jail")
                .withFullDescription("Jails a player at a location.")
                .withPermission("rapunzelcore.jail")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new TimeArgument("duration")
                                .withPermission("rapunzelcore.jail.temp")
                )
                .withOptionalArguments(
                        new GreedyStringArgument("reason")
                )
                .executes((executor, args) -> {
                    Player target = (Player) args.get("player");
                    Duration duration = args.get("duration") == null ? null : (Duration) args.get("duration");
                    String reason = args.get("reason") == null ? "No reason provided" : (String) args.get("reason");

                    if (target == null) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return SINGLE_SUCCESS;
                    }

                    // Check if trying to jail someone with bypass permission
                    if (target.hasPermission("rapunzelcore.moderation.bypass")) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.error.bypass", target.getName()));
                        return SINGLE_SUCCESS;
                    }

                    // Check if jail location is set
                    if (jailLocation == null) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.error.no_location"));
                        return SINGLE_SUCCESS;
                    }

                    // Check if player is already jailed
                    if (jailedPlayers.containsKey(target.getUniqueId())) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.error.already_jailed", target.getName()));
                        return SINGLE_SUCCESS;
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";
                    long durationMs = duration == null ? -1 : duration.toMillis();

                    // Save previous location
                    Location previousLocation = target.getLocation();

                    // Jail the player
                    JailData jailData = new JailData(previousLocation, executorName, reason, durationMs, System.currentTimeMillis());
                    jailedPlayers.put(target.getUniqueId(), jailData);

                    // Teleport to jail
                    target.teleport(jailLocation);

                    String durationStr = durationMs < 0 ? "permanent" : formatDuration(duration);

                    // Broadcast jail
                    Component broadcastMsg = durationMs < 0 
                        ? RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.jail.broadcast.permanent",
                            target.getName(),
                            executorName,
                            reason
                        )
                        : RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.jail.broadcast.temporary",
                            target.getName(),
                            executorName,
                            durationStr,
                            reason
                        );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.jail.notify");

                    // Notify executor
                    executor.sendMessage(durationMs < 0
                        ? RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.success.permanent", target.getName(), reason)
                        : RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.success.temporary", target.getName(), durationStr, reason)
                    );

                    // Notify target
                    target.sendMessage(durationMs < 0
                        ? RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.notify.permanent", executorName, reason)
                        : RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.notify.temporary", executorName, durationStr, reason)
                    );

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Unjail command
        new CommandAPICommand("unjail")
                .withFullDescription("Unjails a player.")
                .withPermission("rapunzelcore.unjail")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .replaceSuggestions((sender, builder) -> {
                                    jailedPlayers.keySet().forEach(uuid -> {
                                        Player p = Bukkit.getPlayer(uuid);
                                        if (p != null) builder.suggest(p.getName());
                                    });
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player target = (Player) args.get("player");

                    if (target == null) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return SINGLE_SUCCESS;
                    }

                    JailData jailData = jailedPlayers.remove(target.getUniqueId());
                    if (jailData == null) {
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unjail.error.not_jailed", target.getName()));
                        return SINGLE_SUCCESS;
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    // Teleport back to previous location
                    target.teleport(jailData.getPreviousLocation());

                    // Broadcast unjail
                    Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(

                            "commands.unjail.broadcast",
                        target.getName(),
                        executorName
                    );
                    Bukkit.broadcast(broadcastMsg, "rapunzelcore.jail.notify");

                    executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unjail.success", target.getName()));
                    target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.unjail.notify", executorName));

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Setjail command
        new CommandAPICommand("setjail")
                .withFullDescription("Sets the jail location.")
                .withPermission("rapunzelcore.jail.set")
                .executesPlayer((player, args) -> {
                    jailLocation = player.getLocation();
                    player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.setjail.success",
                        jailLocation.getWorld().getName(),
                        String.format("%.1f", jailLocation.getX()),
                        String.format("%.1f", jailLocation.getY()),
                        String.format("%.1f", jailLocation.getZ())
                    ));
                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (jailedPlayers.containsKey(player.getUniqueId())) {
            // Check if jail has expired
            JailData jailData = jailedPlayers.get(player.getUniqueId());
            if (jailData != null && jailData.isExpired()) {
                unjailPlayer(player);
                return;
            }

            // Prevent moving outside jail location
            if (jailLocation != null && event.getTo().distance(jailLocation) > 10) {
                event.setCancelled(true);
                player.teleport(jailLocation);
                player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.error.cannot_leave"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (jailedPlayers.containsKey(player.getUniqueId())) {
            // Check if jail has expired
            JailData jailData = jailedPlayers.get(player.getUniqueId());
            if (jailData != null && jailData.isExpired()) {
                unjailPlayer(player);
                return;
            }

            // Prevent teleporting out of jail (unless it's a plugin teleport)
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.error.cannot_teleport"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Keep jail data when player quits - they will be re-jailed when they rejoin
        // This is handled by checking jail status on join if needed
    }

    private void unjailPlayer(Player player) {
        JailData jailData = jailedPlayers.remove(player.getUniqueId());
        if (jailData != null) {
            player.teleport(jailData.getPreviousLocation());
            player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.jail.expired"));
        }
    }

        @Override
    public void unregister() {
        CommandAPI.unregister("jail");
        CommandAPI.unregister("unjail");
        CommandAPI.unregister("setjail");
        // Unregister event listeners
        HandlerList.unregisterAll(this);
        // Release all jailed players
        for (UUID uuid : new ArrayList<>(jailedPlayers.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                unjailPlayer(player);
            }
        }
        jailedPlayers.clear();
    }

    @Override
    public String getName() {
        return "jail";
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

    private static class JailData {
        private final Location previousLocation;
        private final String jailedBy;
        private final String reason;
        private final long durationMs;
        private final long startTime;

        public JailData(Location previousLocation, String jailedBy, String reason, long durationMs, long startTime) {
            this.previousLocation = previousLocation;
            this.jailedBy = jailedBy;
            this.reason = reason;
            this.durationMs = durationMs;
            this.startTime = startTime;
        }

        public Location getPreviousLocation() { return previousLocation; }
        public String getJailedBy() { return jailedBy; }
        public String getReason() { return reason; }
        public boolean isExpired() { return durationMs > 0 && (startTime + durationMs) < System.currentTimeMillis(); }
    }
}
