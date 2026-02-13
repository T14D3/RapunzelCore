package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.PlayerWarning;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.spool.repository.EntityRepository;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarnCommand implements ModerationCommand {
    private final RapunzelCore core;
    private final SpoolDatabase database;
    private final WarningRepository warningRepository;
    private final Map<UUID, List<WarningData>> warningCache = new ConcurrentHashMap<>();

    public WarnCommand(RapunzelCore core, SpoolDatabase database) {
        this.core = core;
        this.database = database;
        this.warningRepository = new WarningRepository();
        initDatabase();
    }

    private void initDatabase() {
        // Spool auto-creates tables for entities - no manual table creation needed
    }

    @Override
    public void register() {
        new CommandAPICommand("warn")
                .withFullDescription("Warns a player.")
                .withPermission("rapunzelcore.warn")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new GreedyStringArgument("reason")
                )
                .executes((executor, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = args.get("reason") == null ? "No reason provided" : (String) args.get("reason");

                    // Look up the player
                    OfflinePlayer target = Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(playerName))
                            .orElseGet(() -> Bukkit.getOfflinePlayer(playerName));

                    if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", playerName);
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";
                    String executorUuid = executor instanceof Player ? ((Player) executor).getUniqueId().toString() : "CONSOLE";
                    String server = RapunzelPaperCore.getServerName();

                    // Create warning
                    CoreDatabase.runLocked(() -> {
                        try {
                            PlayerWarning warning = new PlayerWarning();
                            warning.setPlayerUuid(target.getUniqueId().toString());
                            warning.setWarnedBy(executorUuid);
                            warning.setReason(reason);
                            warning.setCreatedAt(System.currentTimeMillis());
                            warning.setServer(server);
                            warning.setActive(true);

                            warningRepository.save(warning);
                            CoreDatabase.getEntityManager().flush();

                            // Update cache
                            warningCache.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>())
                                    .add(new WarningData(reason, System.currentTimeMillis(), executorUuid, warning.getId()));

                            // Notify target if online
                            if (target.isOnline() && target.getPlayer() != null) {
                                Player onlineTarget = target.getPlayer();
                                Component warnMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                    "commands.warn.received",
                                    reason,
                                    executorName
                                );
                                onlineTarget.sendMessage(warnMsg);
                            }

                            // Count active warnings
                            int warningCount = countActiveWarnings(target.getUniqueId());

                            // Broadcast warning
                            Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                "commands.warn.broadcast",
                                target.getName(),
                                executorName,
                                reason,
                                String.valueOf(warningCount)
                            );
                            Bukkit.broadcast(broadcastMsg, "rapunzelcore.warn.notify");

                            Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                "commands.warn.success",
                                target.getName(),
                                reason,
                                String.valueOf(warningCount)
                            );
                            executor.sendMessage(successMsg);

                            // Check if warning limit reached
                            int limit = core.getConfiguration().getInt("moderation.warning-limit", 3);
                            if (warningCount >= limit) {
                                handleWarningLimit(target, warningCount);
                            }
                        } catch (Exception e) {
                            RapunzelCore.getLogger().error("Failed to create warning", e);
                            Component errorMsg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.database");
                            executor.sendMessage(errorMsg);
                        }
                    });

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Clear warnings command
        new CommandAPICommand("clearwarnings")
                .withFullDescription("Clears all warnings for a player.")
                .withPermission("rapunzelcore.clearwarnings")
                .withAliases("clearwarns")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    String playerName = (String) args.get("player");

                    // Look up the player
                    OfflinePlayer target = Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(playerName))
                            .orElseGet(() -> Bukkit.getOfflinePlayer(playerName));

                    if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", playerName);
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    String executorName = executor instanceof Player ? ((Player) executor).getName() : "Console";

                    CoreDatabase.runLocked(() -> {
                        try {
                            List<PlayerWarning> warnings = warningRepository.findByPlayerUuid(target.getUniqueId().toString());
                            for (PlayerWarning warning : warnings) {
                                warningRepository.delete(warning);
                            }
                            CoreDatabase.getEntityManager().flush();

                            // Clear cache
                            warningCache.remove(target.getUniqueId());

                            Component broadcastMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                "commands.clearwarnings.broadcast",
                                target.getName(),
                                executorName
                            );
                            Bukkit.broadcast(broadcastMsg, "rapunzelcore.warn.notify");

                            Component successMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                "commands.clearwarnings.success",
                                target.getName()
                            );
                            executor.sendMessage(successMsg);
                        } catch (Exception e) {
                            RapunzelCore.getLogger().error("Failed to clear warnings", e);
                            Component errorMsg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.database");
                            executor.sendMessage(errorMsg);
                        }
                    });

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());

        // Check warnings command
        new CommandAPICommand("warnings")
                .withFullDescription("Check warnings for a player.")
                .withPermission("rapunzelcore.warnings.check")
                .withArguments(
                        new StringArgument("player")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    String playerName = (String) args.get("player");

                    // Look up the player
                    OfflinePlayer target = Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(playerName))
                            .orElseGet(() -> Bukkit.getOfflinePlayer(playerName));

                    if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", playerName);
                        executor.sendMessage(msg);
                        return SINGLE_SUCCESS;
                    }

                    List<PlayerWarning> warnings = CoreDatabase.locked(() -> {
                        try {
                            return warningRepository.findActiveByPlayerUuid(target.getUniqueId().toString());
                        } catch (Exception e) {
                            RapunzelCore.getLogger().error("Failed to load warnings", e);
                            return new ArrayList<PlayerWarning>();
                        }
                    });

                    if (warnings.isEmpty()) {
                        Component msg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.warnings.none",
                            target.getName()
                        );
                        executor.sendMessage(msg);
                    } else {
                        Component header = RapunzelCore.getInstance().getMessageHandler().getMessage(
                            "commands.warnings.header",
                            target.getName(),
                            String.valueOf(warnings.size())
                        );
                        executor.sendMessage(header);

                        for (int i = 0; i < warnings.size(); i++) {
                            PlayerWarning w = warnings.get(i);
                            String warnerName = w.getWarnedBy().equals("CONSOLE") ? "Console" : 
                                Optional.ofNullable(Bukkit.getOfflinePlayer(UUID.fromString(w.getWarnedBy())).getName()).orElse("Unknown");
                            
                            Component warnMsg = RapunzelCore.getInstance().getMessageHandler().getMessage(
                                "commands.warnings.entry",
                                String.valueOf(i + 1),
                                warnerName,
                                w.getReason(),
                                new Date(w.getCreatedAt()).toString()
                            );
                            executor.sendMessage(warnMsg);
                        }
                    }

                    return SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

        @Override
    public void unregister() {
        CommandAPI.unregister("warn");
        CommandAPI.unregister("clearwarnings");
        CommandAPI.unregister("warnings");
    }

    @Override
    public String getName() {
        return "warn";
    }

    private int countActiveWarnings(UUID playerUuid) {
        return CoreDatabase.locked(() -> {
            try {
                return warningRepository.findActiveByPlayerUuid(playerUuid.toString()).size();
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to count warnings", e);
                return 0;
            }
        });
    }

    private void handleWarningLimit(OfflinePlayer target, int warningCount) {
        String action = core.getConfiguration().getString("moderation.warning-limit-action", "kick");
        String reason = "Exceeded warning limit (" + warningCount + " warnings)";

        switch (action.toLowerCase()) {
            case "ban" -> {
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().kick(Component.text("You have been banned: " + reason));
                }
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), reason, null, "System");
            }
            case "mute" -> {
                // Would need to integrate with MuteManager
                RapunzelCore.getLogger().info("Player " + target.getName() + " exceeded warning limit - should be muted");
            }
            default -> {
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().kick(Component.text("You have been kicked: " + reason));
                }
            }
        }
    }

    /**
     * Loads warning data for a player
     */
    public void loadPlayerData(UUID playerUuid) {
        CoreDatabase.runLockedAsync(() -> {
            try {
                List<PlayerWarning> warnings = warningRepository.findActiveByPlayerUuid(playerUuid.toString());
                List<WarningData> dataList = new ArrayList<>();
                for (PlayerWarning w : warnings) {
                    dataList.add(new WarningData(w.getReason(), w.getCreatedAt(), w.getWarnedBy(), w.getId()));
                }
                warningCache.put(playerUuid, dataList);
            } catch (Exception e) {
                RapunzelCore.getLogger().error("Failed to load warning data for: " + playerUuid, e);
            }
        });
    }

    /**
     * Unloads warning data for a player
     */
    public void unloadPlayerData(UUID playerUuid) {
        warningCache.remove(playerUuid);
    }

    /**
     * Repository for PlayerWarning entity operations
     */
    private class WarningRepository extends EntityRepository<PlayerWarning> {
        WarningRepository() {
            super(CoreDatabase.getEntityManager(), PlayerWarning.class);
        }

        List<PlayerWarning> findByPlayerUuid(String playerUuid) {
            return findAll().stream()
                .filter(w -> w.getPlayerUuid().equals(playerUuid))
                .toList();
        }

        List<PlayerWarning> findActiveByPlayerUuid(String playerUuid) {
            return findAll().stream()
                .filter(w -> w.getPlayerUuid().equals(playerUuid) && w.isActive())
                .toList();
        }
    }

    /**
     * Data class for warning information
     */
    public static class WarningData {
        private final String reason;
        private final long createdAt;
        private final String warnedBy;
        private final long id;

        public WarningData(String reason, long createdAt, String warnedBy, long id) {
            this.reason = reason;
            this.createdAt = createdAt;
            this.warnedBy = warnedBy;
            this.id = id;
        }

        public String getReason() {
            return reason;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public String getWarnedBy() {
            return warnedBy;
        }

        public long getId() {
            return id;
        }
    }
}
