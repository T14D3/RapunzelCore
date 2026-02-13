package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.InventoryProfile;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Commands for managing inventory snapshots.
 */
public class SnapshotCommands {
    private final RapunzelPaperCore paperCore;
    private final InventoryRepository repository;
    private final InventorySerializer serializer;
    private final InventoryConfig config;
    private final SnapshotViewerGUI viewerGUI;

    public SnapshotCommands(RapunzelPaperCore paperCore, InventoryConfig config, InventorySerializer serializer) {
        this.paperCore = paperCore;
        this.repository = InventoryRepository.getInstance();
        this.serializer = serializer;
        this.config = config;
        this.viewerGUI = new SnapshotViewerGUI(paperCore, serializer);
    }

    public void register() {
        // /snapshot create [name] [reason]
        new CommandAPICommand("snapshot").withFullDescription("Manage inventory snapshots").withPermission("rapunzelcore.snapshot").withSubcommand(new CommandAPICommand("create").withPermission("rapunzelcore.snapshot.create").withOptionalArguments(new StringArgument("name")).withOptionalArguments(new GreedyStringArgument("reason")).executesPlayer((player, args) -> {
            String name = args.get("name") != null ? (String) args.get("name") : null;
            String reason = args.get("reason") != null ? (String) args.get("reason") : null;

            createSnapshot(player, player, name, "MANUAL", player.getUniqueId().toString(), reason);
            return Command.SINGLE_SUCCESS;
        })).withSubcommand(new CommandAPICommand("list").withPermission("rapunzelcore.snapshot.list").withOptionalArguments(new StringArgument("player").replaceSuggestions((sender, builder) -> {
            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
            return builder.buildFuture();
        })).executes((sender, args) -> {
            String playerName = (String) args.get("player");
            if (playerName == null) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("You must specify a player", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                listSnapshots(sender, ((Player) sender).getUniqueId());
            } else {
                if (!sender.hasPermission("rapunzelcore.snapshot.list.other")) {
                    sender.sendMessage(Component.text("You don't have permission to view other players' snapshots", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
                if (target == null) {
                    target = Bukkit.getOfflinePlayer(playerName);
                }
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                listSnapshots(sender, target.getUniqueId());
            }
            return Command.SINGLE_SUCCESS;
        })).withSubcommand(new CommandAPICommand("view").withPermission("rapunzelcore.snapshot.view").withArguments(new IntegerArgument("id")).executes((sender, args) -> {
            int id = (int) args.get("id");
            viewSnapshot(sender, id);
            return Command.SINGLE_SUCCESS;
        })).withSubcommand(new CommandAPICommand("restore").withPermission("rapunzelcore.snapshot.restore").withArguments(new IntegerArgument("id")).withOptionalArguments(new StringArgument("player").replaceSuggestions((sender, builder) -> {
            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
            return builder.buildFuture();
        })).executes((sender, args) -> {
            int id = (int) args.get("id");
            String playerName = (String) args.get("player");

            Player target;
            if (playerName == null) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("You must specify a player or run as a player", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                target = (Player) sender;
            } else {
                if (!sender.hasPermission("rapunzelcore.snapshot.restore.other")) {
                    sender.sendMessage(Component.text("You don't have permission to restore snapshots for other players", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                target = Bukkit.getPlayer(playerName);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not online: " + playerName, NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
            }

            restoreSnapshot(sender, target, id);
            return Command.SINGLE_SUCCESS;
        })).withSubcommand(new CommandAPICommand("delete").withPermission("rapunzelcore.snapshot.delete").withArguments(new IntegerArgument("id")).executes((sender, args) -> {
            int id = (int) args.get("id");
            deleteSnapshot(sender, id);
            return Command.SINGLE_SUCCESS;
        })).withSubcommand(new CommandAPICommand("prune").withPermission("rapunzelcore.snapshot.prune").withOptionalArguments(new StringArgument("player").replaceSuggestions((sender, builder) -> {
            Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
            return builder.buildFuture();
        })).executes((sender, args) -> {
            String playerName = (String) args.get("player");
            if (playerName == null) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Component.text("You must specify a player", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                pruneSnapshots(sender, ((Player) sender).getUniqueId());
            } else {
                if (!sender.hasPermission("rapunzelcore.snapshot.prune.other")) {
                    sender.sendMessage(Component.text("You don't have permission to prune other players' snapshots", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(playerName);
                if (target == null) {
                    target = Bukkit.getOfflinePlayer(playerName);
                }
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                }
                pruneSnapshots(sender, target.getUniqueId());
            }
            return Command.SINGLE_SUCCESS;
        })).register((JavaPlugin) RapunzelCore.getInstance());
    }

    public void unregister() {
        CommandAPI.unregister("snapshot");
    }

    private void createSnapshot(Player player, Player target, String name, String type, String createdBy, String reason) {
        repository.createSnapshotAsync(target, name, type, createdBy, reason, serializer, config).thenAccept(profile -> {
            Bukkit.getScheduler().runTask(paperCore, () -> {
                if (profile != null) {
                    player.sendMessage(Component.text("Snapshot created successfully!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Failed to create snapshot", NamedTextColor.RED));
                }
            });
        }).exceptionally(error -> {
            RapunzelCore.getLogger().error("Failed to create snapshot", error);
            Bukkit.getScheduler().runTask(paperCore, () -> {
                player.sendMessage(Component.text("Failed to create snapshot: " + error.getMessage(), NamedTextColor.RED));
            });
            return null;
        });
    }

    private void listSnapshots(CommandSender sender, UUID playerUuid) {
        repository.listSnapshotsAsync(playerUuid).thenAccept(snapshots -> {
            Bukkit.getScheduler().runTask(paperCore, () -> {
                if (snapshots.isEmpty()) {
                    sender.sendMessage(Component.text("No snapshots found.", NamedTextColor.YELLOW));
                    return;
                }

                sender.sendMessage(Component.text("=== Snapshots ===", NamedTextColor.GOLD));
                for (InventoryProfile snapshot : snapshots) {
                    String name = snapshot.getSnapshotName().isEmpty() ? "Unnamed" : snapshot.getSnapshotName();
                    String date = new Date(snapshot.getCreatedAt()).toString();
                    Component line = Component.text("[" + snapshot.getId() + "] ", NamedTextColor.GRAY).append(Component.text(name, NamedTextColor.WHITE)).append(Component.text(" (" + snapshot.getSnapshotType() + ") ", NamedTextColor.AQUA)).append(Component.text("- " + date, NamedTextColor.GRAY));
                    sender.sendMessage(line);
                }
            });
        });
    }

    private void viewSnapshot(CommandSender sender, long id) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players", NamedTextColor.RED));
            return;
        }

        repository.getSnapshotAsync(id).thenAccept(snapshot -> {
            Bukkit.getScheduler().runTask(paperCore, () -> {
                if (snapshot == null) {
                    player.sendMessage(Component.text("Snapshot not found: " + id, NamedTextColor.RED));
                    return;
                }

                // Check permission for viewing other players' snapshots
                if (!snapshot.getPlayerUuid().equals(player.getUniqueId().toString()) && !player.hasPermission("rapunzelcore.snapshot.view.other")) {
                    player.sendMessage(Component.text("You don't have permission to view this snapshot", NamedTextColor.RED));
                    return;
                }

                viewerGUI.openViewer(player, snapshot);
            });
        });
    }

    private void restoreSnapshot(CommandSender sender, Player target, long id) {
        repository.getSnapshotAsync(id).thenAccept(snapshot -> {
            Bukkit.getScheduler().runTask(paperCore, () -> {
                if (snapshot == null) {
                    sender.sendMessage(Component.text("Snapshot not found: " + id, NamedTextColor.RED));
                    return;
                }

                // Check permission for restoring other players' snapshots
                if (!snapshot.getPlayerUuid().equals(target.getUniqueId().toString()) && !sender.hasPermission("rapunzelcore.snapshot.restore.other")) {
                    sender.sendMessage(Component.text("You don't have permission to restore this snapshot for another player", NamedTextColor.RED));
                    return;
                }

                // Check if player restore is allowed
                if (sender == target && !config.allowPlayerRestore) {
                    sender.sendMessage(Component.text("Players cannot restore their own snapshots", NamedTextColor.RED));
                    return;
                }

                // Apply snapshot
                applySnapshot(target, snapshot);

                sender.sendMessage(Component.text("Snapshot restored successfully for " + target.getName(), NamedTextColor.GREEN));
                if (sender != target) {
                    target.sendMessage(Component.text("Your inventory has been restored from a snapshot by " + sender.getName(), NamedTextColor.YELLOW));
                }
            });
        });
    }

    private void deleteSnapshot(CommandSender sender, long id) {
        repository.getSnapshotAsync(id).thenAccept(snapshot -> {
            if (snapshot == null) {
                Bukkit.getScheduler().runTask(paperCore, () -> {
                    sender.sendMessage(Component.text("Snapshot not found: " + id, NamedTextColor.RED));
                });
                return;
            }

            // Check permission for deleting other players' snapshots
            String senderUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
            if (!snapshot.getPlayerUuid().equals(senderUuid) && !sender.hasPermission("rapunzelcore.snapshot.delete.other")) {
                Bukkit.getScheduler().runTask(paperCore, () -> {
                    sender.sendMessage(Component.text("You don't have permission to delete this snapshot", NamedTextColor.RED));
                });
                return;
            }

            repository.deleteSnapshotAsync(id).thenAccept(deleted -> {
                Bukkit.getScheduler().runTask(paperCore, () -> {
                    if (deleted) {
                        sender.sendMessage(Component.text("Snapshot deleted successfully", NamedTextColor.GREEN));
                    } else {
                        sender.sendMessage(Component.text("Failed to delete snapshot", NamedTextColor.RED));
                    }
                });
            });
        });
    }

    private void pruneSnapshots(CommandSender sender, UUID playerUuid) {
        int maxCount = config.maxSnapshotsPerPlayer;
        long maxAgeMillis = config.retentionDays * 24L * 60L * 60L * 1000L;

        repository.pruneSnapshotsAsync(playerUuid, maxCount, maxAgeMillis).thenAccept(deleted -> {
            Bukkit.getScheduler().runTask(paperCore, () -> {
                sender.sendMessage(Component.text("Pruned " + deleted + " old snapshots", NamedTextColor.GREEN));
            });
        });
    }

    private void applySnapshot(Player player, InventoryProfile snapshot) {
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = serializer.decodeItems(new String(snapshot.getInventoryData()));
        ItemStack[] armor = serializer.decodeItems(new String(snapshot.getArmorData()));
        ItemStack[] extra = serializer.decodeItems(new String(snapshot.getExtraData()));
        ItemStack[] ender = serializer.decodeItems(new String(snapshot.getEnderChestData()));
        List<PotionEffect> effects = serializer.decodeEffects(new String(snapshot.getPotionData()));

        if (storage != null) {
            inv.setStorageContents(fit(storage, inv.getStorageContents().length));
        }
        if (armor != null) {
            inv.setArmorContents(fit(armor, inv.getArmorContents().length));
        }
        if (config.includeOffhand && extra != null) {
            inv.setExtraContents(fit(extra, inv.getExtraContents().length));
        }
        if (config.includeEnderChest && ender != null) {
            player.getEnderChest().setContents(fit(ender, player.getEnderChest().getContents().length));
        }

        if (config.applyPotions) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect, true);
            }
        }

        if (config.applyStats) {
            double max = player.getMaxHealth();
            double newHealth = Math.max(0.5D, Math.min(snapshot.getHealth(), max));
            player.setHealth(newHealth);
            player.setFoodLevel(snapshot.getFoodLevel());
            player.setSaturation(snapshot.getSaturation());
            player.setLevel(snapshot.getExpLevel());
            player.setExp(Math.max(0.0f, Math.min(1.0f, snapshot.getExpProgress())));
        }

        int held = snapshot.getHeldSlot();
        if (held >= 0 && held < inv.getSize()) {
            inv.setHeldItemSlot(held);
        }
        player.updateInventory();
    }

    private static ItemStack[] fit(ItemStack[] source, int size) {
        if (source == null) return new ItemStack[size];
        if (source.length == size) return source;
        ItemStack[] result = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            if (i < source.length) result[i] = source[i];
        }
        return result;
    }
}
