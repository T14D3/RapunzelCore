package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.modules.inventories.network.InventorySyncPayload;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class InventoriesModule implements Module, Listener, AutoCloseable {
    private boolean enabled;
    private RapunzelPaperCore paperCore;
    private InventoryConfig settings;
    private InventorySerializer serializer;
    private InventoryRepository repository;
    private SnapshotCommands snapshotCommands;
    private SnapshotViewerGUI snapshotViewerGUI;
    private AutoSnapshotTask autoSnapshotTask;
    private ScheduledTask autoSnapshotBukkitTask;
    private NetworkEventBus bus;
    private NetworkEventBus.Subscription syncSub;

    private final Map<UUID, InventoryContext> activeContexts = new ConcurrentHashMap<>();
    private final Map<UUID, String> requestedContext = new ConcurrentHashMap<>();

    private enum SaveReason {
        JOIN,
        QUIT,
        WORLD_CHANGE,
        GAMEMODE_CHANGE,
        MANUAL
    }

    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    @Override
    public void enable(RapunzelCore core) {
        if (enabled) return;
        this.paperCore = (RapunzelPaperCore) core;
        this.settings = new InventoryConfig(core.getConfiguration());
        this.serializer = new InventorySerializer(paperCore.getSLF4JLogger());
        this.repository = InventoryRepository.getInstance();

        if (settings.networkInvalidation) {
            this.bus = new NetworkEventBus(paperCore.getMessenger());
            this.syncSub = bus.register(
                    NetworkChannels.INVENTORIES_SYNC,
                    InventorySyncPayload.class,
                    (payload, source) -> {
                        if (payload == null) return;
                        repository.invalidate(payload.playerUuid(), payload.contextKey());
                    }
            );
        }

        // Register snapshot commands if enabled
        if (settings.snapshotsEnabled) {
            this.snapshotCommands = new SnapshotCommands(paperCore, settings, serializer);
            this.snapshotCommands.register();
            this.snapshotViewerGUI = new SnapshotViewerGUI(paperCore, serializer);

            // Schedule auto snapshot task if enabled
            if (settings.autoSnapshotEnabled) {
                this.autoSnapshotTask = new AutoSnapshotTask(settings, serializer);
                Duration autosaveInterval = Duration.ofMinutes(settings.autoSnapshotIntervalMinutes);
                this.autoSnapshotBukkitTask = Rapunzel.context().scheduler().runRepeating(
                        autosaveInterval,
                        autosaveInterval,
                        autoSnapshotTask
                );
            }
        }

        Bukkit.getPluginManager().registerEvents(this, paperCore);
        enabled = true;

        // Reload-safe: ensure currently online players are synced.
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryContext context = settings.resolve(player.getWorld(), player.getGameMode());
            activeContexts.put(player.getUniqueId(), context);
            loadContext(player, context, true);
        }
    }

    @Override
    public void disable() {
        if (!enabled) return;
        if (syncSub != null) {
            syncSub.close();
            syncSub = null;
        }
        bus = null;

        // Cleanup snapshot components
        if (autoSnapshotBukkitTask != null) {
            autoSnapshotBukkitTask.cancel();
            autoSnapshotBukkitTask = null;
        }
        if (autoSnapshotTask != null) {
            autoSnapshotTask = null;
        }
        if (snapshotViewerGUI != null) {
            snapshotViewerGUI.close();
            snapshotViewerGUI = null;
        }
        if (snapshotCommands != null) {
            snapshotCommands.unregister();
            snapshotCommands = null;
        }

        HandlerList.unregisterAll(this);
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getName() {
        return "inventories";
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        InventoryContext context = settings.resolve(player.getWorld(), player.getGameMode());
        activeContexts.put(player.getUniqueId(), context);
        Bukkit.getScheduler().runTaskLater(paperCore, () -> loadContext(player, context, true), settings.applyDelayTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        InventoryContext context = activeContexts.getOrDefault(
                player.getUniqueId(),
                settings.resolve(player.getWorld(), player.getGameMode())
        );
        saveContext(player, context, SaveReason.QUIT);
        activeContexts.remove(player.getUniqueId());
        requestedContext.remove(player.getUniqueId());

        // Remove from auto snapshot tracking
        if (autoSnapshotTask != null) {
            autoSnapshotTask.removePlayer(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        InventoryContext previous = activeContexts.getOrDefault(id, settings.resolve(event.getFrom(), player.getGameMode()));
        InventoryContext next = settings.resolve(player.getWorld(), player.getGameMode());

        if (previous.key().equalsIgnoreCase(next.key())) {
            activeContexts.put(id, next);
            return;
        }

        saveContext(player, previous, SaveReason.WORLD_CHANGE);
        activeContexts.put(id, next);
        Bukkit.getScheduler().runTaskLater(paperCore, () -> loadContext(player, next, false), settings.applyDelayTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGamemodeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        InventoryContext previous = activeContexts.getOrDefault(id, settings.resolve(player.getWorld(), player.getGameMode()));
        InventoryContext next = settings.resolve(player.getWorld(), event.getNewGameMode());

        if (!previous.key().equalsIgnoreCase(next.key())) {
            saveContext(player, previous, SaveReason.GAMEMODE_CHANGE);
            activeContexts.put(id, next);
            // Delay apply so the gamemode change has been processed.
            Bukkit.getScheduler().runTaskLater(paperCore, () -> loadContext(player, next, false), settings.applyDelayTicks);
        } else {
            activeContexts.put(id, next);
        }
    }

    private void saveContext(Player player, InventoryContext context, SaveReason reason) {
        if (player == null || context == null || !shouldSave(reason)) return;

        InventorySnapshot snapshot = captureSnapshot(player);
        InventoryRecord record = toRecord(context, snapshot);
        UUID id = player.getUniqueId();

        repository.saveAsync(id, record, settings.flushImmediately)
                .thenAccept(saved -> {
                    if (saved != null && bus != null && settings.networkInvalidation) {
                        InventorySyncPayload payload = new InventorySyncPayload(id.toString(), context.key(), saved.updatedAt());
                        Bukkit.getScheduler().runTask(paperCore, () -> bus.sendToAll(NetworkChannels.INVENTORIES_SYNC, payload));
                    }
                })
                .exceptionally(error -> {
                    paperCore.getLogger().warning("Failed to save inventory for " + id + ": " + error.getMessage());
                    return null;
                });
    }

    private void loadContext(Player player, InventoryContext context, boolean joining) {
        if (player == null || context == null) return;
        UUID id = player.getUniqueId();
        requestedContext.put(id, context.key());

        CompletableFuture<InventoryRecord> future = repository.loadAsync(id, context);
        future.whenComplete((record, error) -> {
            if (error != null) {
                paperCore.getLogger().warning("Failed to load inventory for " + id + ": " + error.getMessage());
                return;
            }
            Bukkit.getScheduler().runTask(paperCore, () -> {
                String expected = requestedContext.get(id);
                if (expected == null || !expected.equalsIgnoreCase(context.key())) {
                    return; // stale
                }
                if (record == null) {
                    handleMissingProfile(player, context, joining);
                    return;
                }
                applyRecord(player, record);
                activeContexts.put(id, context);
            });
        });
    }

    private void handleMissingProfile(Player player, InventoryContext context, boolean joining) {
        if (joining && settings.saveOnJoin && settings.keepJoinInventoryIfNew) {
            saveContext(player, context, SaveReason.JOIN);
            activeContexts.put(player.getUniqueId(), context);
            return;
        }

        if (settings.startEmptyForNewContext) {
            InventorySnapshot empty = InventorySnapshot.emptyFor(player);
            applySnapshot(player, empty);
            saveContext(player, context, SaveReason.MANUAL);
            activeContexts.put(player.getUniqueId(), context);
            return;
        }

        activeContexts.put(player.getUniqueId(), context);
    }

    private boolean shouldSave(SaveReason reason) {
        return switch (reason) {
            case JOIN -> settings.saveOnJoin;
            case QUIT -> settings.saveOnQuit;
            case WORLD_CHANGE -> settings.saveOnWorldChange;
            case GAMEMODE_CHANGE -> settings.saveOnGamemodeChange;
            case MANUAL -> true;
        };
    }

    private InventorySnapshot captureSnapshot(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] storage = clone(inv.getStorageContents());
        ItemStack[] armor = clone(inv.getArmorContents());
        ItemStack[] extra = settings.includeOffhand ? clone(inv.getExtraContents()) : new ItemStack[0];
        ItemStack[] ender = settings.includeEnderChest ? clone(player.getEnderChest().getContents()) : new ItemStack[0];
        java.util.List<PotionEffect> effects = settings.applyPotions ? player.getActivePotionEffects().stream().toList() : java.util.List.of();

        double health = player.getHealth();
        int food = player.getFoodLevel();
        float saturation = player.getSaturation();
        int level = player.getLevel();
        float exp = player.getExp();

        return new InventorySnapshot(
                storage,
                armor,
                extra,
                ender,
                effects,
                health,
                food,
                saturation,
                level,
                exp,
                inv.getHeldItemSlot()
        );
    }

    private InventoryRecord toRecord(InventoryContext context, InventorySnapshot snapshot) {
        long now = System.currentTimeMillis();
        return new InventoryRecord(
                context,
                serializer.encodeItems(snapshot.storage()),
                serializer.encodeItems(snapshot.armor()),
                serializer.encodeItems(snapshot.extra()),
                serializer.encodeItems(snapshot.enderChest()),
                serializer.encodeEffects(snapshot.effects()),
                settings.applyStats ? snapshot.health() : 0.0,
                settings.applyStats ? snapshot.foodLevel() : 0,
                settings.applyStats ? snapshot.saturation() : 0.0f,
                settings.applyStats ? snapshot.expLevel() : 0,
                settings.applyStats ? snapshot.expProgress() : 0.0f,
                snapshot.heldSlot(),
                now
        );
    }

    private void applyRecord(Player player, InventoryRecord record) {
        InventorySnapshot snapshot = new InventorySnapshot(
                serializer.decodeItems(new String(record.inventoryData())),
                serializer.decodeItems(new String(record.armorData())),
                serializer.decodeItems(new String(record.extraData())),
                serializer.decodeItems(new String(record.enderChestData())),
                settings.applyPotions ? serializer.decodeEffects(new String(record.potionData())) : java.util.List.of(),
                record.health(),
                record.foodLevel(),
                record.saturation(),
                record.expLevel(),
                record.expProgress(),
                record.heldSlot()
        );
        applySnapshot(player, snapshot);
    }

    private void applySnapshot(Player player, InventorySnapshot snapshot) {
        PlayerInventory inv = player.getInventory();
        if (snapshot.storage() != null) {
            inv.setStorageContents(fit(snapshot.storage(), inv.getStorageContents().length));
        }
        if (snapshot.armor() != null) {
            inv.setArmorContents(fit(snapshot.armor(), inv.getArmorContents().length));
        }
        if (settings.includeOffhand && snapshot.extra() != null) {
            inv.setExtraContents(fit(snapshot.extra(), inv.getExtraContents().length));
        }
        if (settings.includeEnderChest && snapshot.enderChest() != null) {
            player.getEnderChest().setContents(fit(snapshot.enderChest(), player.getEnderChest().getContents().length));
        }

        if (settings.applyPotions) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            for (PotionEffect effect : snapshot.effects()) {
                player.addPotionEffect(effect, true);
            }
        }

        if (settings.applyStats) {
            double max = player.getMaxHealth();
            double newHealth = Math.max(0.5D, Math.min(snapshot.health(), max));
            player.setHealth(newHealth);
            player.setFoodLevel(snapshot.foodLevel());
            player.setSaturation(snapshot.saturation());
            player.setLevel(snapshot.expLevel());
            player.setExp(Math.max(0.0f, Math.min(1.0f, snapshot.expProgress())));
        }

        int held = snapshot.heldSlot();
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

    private static ItemStack[] clone(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            ItemStack item = items[i];
            copy[i] = item == null ? null : item.clone();
        }
        return copy;
    }

    public void close() {
        if (syncSub != null) syncSub.close();
        syncSub = null;
        bus = null;

        if (autoSnapshotBukkitTask != null) {
            autoSnapshotBukkitTask.cancel();
            autoSnapshotBukkitTask = null;
        }
        if (snapshotViewerGUI != null) {
            snapshotViewerGUI.close();
            snapshotViewerGUI = null;
        }
        if (snapshotCommands != null) {
            snapshotCommands.unregister();
            snapshotCommands = null;
        }

        HandlerList.unregisterAll(this);
    }

    /**
     * Gets the inventory repository for external access.
     */
    public InventoryRepository getRepository() {
        return repository;
    }

    /**
     * Creates a snapshot for a player (for moderation integration).
     * Uses the centralized repository method.
     */
    public void createSnapshot(Player player, String name, String type, String createdBy, String reason) {
        if (!settings.snapshotsEnabled || repository == null) return;

        repository.createSnapshotAsync(player, name, type, createdBy, reason, serializer, settings)
                .thenAccept(profile -> {
                    if (profile != null) {
                        RapunzelCore.getLogger().info("Created {} snapshot for player: {}", type, player.getName());
                    }
                })
                .exceptionally(error -> {
                    RapunzelCore.getLogger().error("Failed to create snapshot for player: {}", player.getName(), error);
                    return null;
                });
    }
}
