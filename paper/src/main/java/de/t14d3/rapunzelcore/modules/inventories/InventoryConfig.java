package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzellib.config.YamlConfig;
import org.bukkit.GameMode;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parsed inventories module configuration.
 */
final class InventoryConfig {
    // Core sync settings
    final boolean saveOnJoin;
    final boolean saveOnQuit;
    final boolean saveOnWorldChange;
    final boolean saveOnGamemodeChange;
    final boolean flushImmediately;
    final boolean applyStats;
    final boolean applyPotions;
    final boolean includeEnderChest;
    final boolean includeOffhand;
    final boolean networkInvalidation;
    final boolean keepJoinInventoryIfNew;
    final boolean startEmptyForNewContext;
    final int applyDelayTicks;
    final String defaultWorldGroup;

    // Snapshot settings
    final boolean snapshotsEnabled;
    final boolean autoSnapshotEnabled;
    final int autoSnapshotIntervalMinutes;
    final int maxSnapshotsPerPlayer;
    final int maxManualSnapshots;
    final int retentionDays;
    final boolean allowPlayerRestore;

    private final Map<String, String> worldGroupByWorld;
    private final Map<String, String> gameModeAliases;

    InventoryConfig(YamlConfig config) {
        // Core sync settings
        this.saveOnJoin = config.getBoolean("sync.save-on-join", true);
        this.saveOnQuit = config.getBoolean("sync.save-on-quit", true);
        this.saveOnWorldChange = config.getBoolean("sync.save-on-world-change", true);
        this.saveOnGamemodeChange = config.getBoolean("sync.save-on-gamemode-change", true);
        this.flushImmediately = config.getBoolean("sync.flush-immediately", true);
        this.applyStats = config.getBoolean("sync.apply-stats", true);
        this.applyPotions = config.getBoolean("sync.apply-potions", true);
        this.includeEnderChest = config.getBoolean("sync.include-ender-chest", true);
        this.includeOffhand = config.getBoolean("sync.include-offhand", true);
        this.networkInvalidation = config.getBoolean("sync.network-invalidation", true);

        this.keepJoinInventoryIfNew = config.getBoolean("defaults.keep-join-inventory-if-new", true);
        this.startEmptyForNewContext = config.getBoolean("defaults.start-empty-for-new-context", true);
        this.applyDelayTicks = config.getInt("defaults.apply-delay-ticks", 2);
        this.defaultWorldGroup = InventoryContext.normalize(config.getString("defaults.world-group", "default"));

        // Snapshot settings
        this.snapshotsEnabled = config.getBoolean("snapshots.enabled", true);
        this.autoSnapshotEnabled = config.getBoolean("snapshots.auto-enabled", true);
        this.autoSnapshotIntervalMinutes = config.getInt("snapshots.auto-interval-minutes", 30);
        this.maxSnapshotsPerPlayer = config.getInt("snapshots.max-per-player", 50);
        this.maxManualSnapshots = config.getInt("snapshots.max-manual", 10);
        this.retentionDays = config.getInt("snapshots.retention-days", 30);
        this.allowPlayerRestore = config.getBoolean("snapshots.allow-player-restore", false);

        this.worldGroupByWorld = loadWorldGroups(config);
        this.gameModeAliases = loadGamemodeAliases(config);
    }

    InventoryContext resolve(World world, GameMode gameMode) {
        String worldGroup = worldGroup(world == null ? null : world.getName());
        String gameModeKey = gameModeKey(gameMode == null ? null : gameMode.name());
        return new InventoryContext(worldGroup, gameModeKey);
    }

    String worldGroup(String worldName) {
        if (worldName == null) return defaultWorldGroup;
        String group = worldGroupByWorld.get(worldName.trim().toLowerCase(Locale.ROOT));
        if (group != null && !group.isBlank()) return group;
        return defaultWorldGroup;
    }

    String gameModeKey(String value) {
        if (value == null) return "unknown";
        String normalized = InventoryContext.normalize(value);
        String mapped = gameModeAliases.get(normalized);
        return mapped != null ? mapped : normalized;
    }

    private Map<String, String> loadWorldGroups(YamlConfig config) {
        Map<String, String> map = new HashMap<>();
        Set<String> groups = childKeys(config, "world-groups");
        for (String group : groups) {
            String normalizedGroup = InventoryContext.normalize(group);
            List<String> worlds = config.getStringList("world-groups." + group + ".worlds");
            if (worlds.isEmpty()) continue;
            for (String world : worlds) {
                if (world == null || world.isBlank()) continue;
                map.put(world.trim().toLowerCase(Locale.ROOT), normalizedGroup);
            }
        }
        return map;
    }

    private Map<String, String> loadGamemodeAliases(YamlConfig config) {
        Map<String, String> map = new HashMap<>();
        Set<String> groups = childKeys(config, "gamemode-groups");
        if (groups.isEmpty()) {
            // Default aliases
            for (GameMode gm : GameMode.values()) {
                String key = InventoryContext.normalize(gm.name());
                map.put(key, key);
            }
            return map;
        }

        for (String group : groups) {
            String normalized = InventoryContext.normalize(group);
            List<String> aliases = config.getStringList("gamemode-groups." + group + ".aliases");
            if (aliases.isEmpty()) {
                map.put(normalized, normalized);
                continue;
            }
            for (String alias : aliases) {
                if (alias == null || alias.isBlank()) continue;
                map.put(InventoryContext.normalize(alias), normalized);
            }
        }
        return map;
    }

    private static Set<String> childKeys(YamlConfig config, String root) {
        if (config == null || root == null || root.isBlank()) return Set.of();
        String prefix = root.endsWith(".") ? root : (root + ".");
        Set<String> out = new HashSet<>();
        for (String key : config.keys(true)) {
            if (key == null || !key.startsWith(prefix)) continue;
            String rest = key.substring(prefix.length());
            int dot = rest.indexOf('.');
            String child = dot >= 0 ? rest.substring(0, dot) : rest;
            if (!child.isBlank()) out.add(child);
        }
        return out;
    }
}
