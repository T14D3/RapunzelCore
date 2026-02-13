package de.t14d3.rapunzelcore.modules.inventories;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.InventoryProfile;
import de.t14d3.spool.cache.CacheEvent;
import de.t14d3.spool.repository.EntityRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified repository for inventory profiles and snapshots.
 * Handles both current inventory state and named snapshots.
 */
public final class InventoryRepository extends EntityRepository<InventoryProfile> {
 private static final InventoryRepository instance = new InventoryRepository();

 private final Map<String, InventoryRecord> cache = new ConcurrentHashMap<>();
 private volatile boolean syncRegistered;

 private InventoryRepository() {
 super(CoreDatabase.getEntityManager(), InventoryProfile.class);
 registerSyncListenerIfAvailable();
 }

 public static InventoryRepository getInstance() {
 return instance;
 }

 // ==================== Current Inventory State Operations ====================

 /**
 * Loads current inventory state for a player in a specific context.
 */
 public CompletableFuture<InventoryRecord> loadAsync(UUID playerUuid, InventoryContext context) {
 if (playerUuid == null || context == null) return CompletableFuture.completedFuture(null);
 String playerKey = playerUuid.toString();
 String cacheKey = cacheKey(playerKey, context.key());
 InventoryRecord cached = cache.get(cacheKey);
 if (cached != null) return CompletableFuture.completedFuture(cached);

 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 InventoryProfile profile = findCurrentProfile(playerKey, context.key());
 if (profile == null) return null;
 InventoryRecord record = toRecord(profile);
 cache.put(cacheKey, record);
 return record;
 }));
 }

 /**
 * Saves current inventory state for a player.
 */
 public CompletableFuture<InventoryRecord> saveAsync(UUID playerUuid, InventoryRecord record, boolean flush) {
 if (playerUuid == null || record == null || record.context() == null) {
 return CompletableFuture.completedFuture(null);
 }
 String playerKey = playerUuid.toString();
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 InventoryProfile profile = findCurrentProfile(playerKey, record.contextKey());
 if (profile == null) {
 profile = new InventoryProfile();
 profile.setPlayerUuid(playerKey);
 profile.setContextKey(record.contextKey());
 profile.setSnapshotType("CURRENT");
 }
 profile.setWorldGroup(record.worldGroup());
 profile.setGameMode(record.gameMode());
 profile.setInventoryData(record.inventoryData().getBytes());
 profile.setArmorData(record.armorData().getBytes());
 profile.setExtraData(record.extraData().getBytes());
 profile.setEnderChestData(record.enderChestData().getBytes());
 profile.setPotionData(record.potionData().getBytes());
 profile.setHealth(record.health());
 profile.setFoodLevel(record.foodLevel());
 profile.setSaturation(record.saturation());
 profile.setExpLevel(record.expLevel());
 profile.setExpProgress(record.expProgress());
 profile.setHeldSlot(record.heldSlot());
 profile.setUpdatedAt(record.updatedAt());

 save(profile);
 if (flush) {
 CoreDatabase.getEntityManager().flush();
 }

 InventoryRecord cached = toRecord(profile);
 cache.put(cacheKey(playerKey, record.contextKey()), cached);
 return cached;
 }));
 }

 // ==================== Snapshot Operations ====================

 /**
 * Creates a complete snapshot for a player with all inventory data.
 * This is the centralized method that handles all snapshot creation logic.
 *
 * @param player The player to snapshot
 * @param name Optional name for the snapshot
 * @param type Type of snapshot (MANUAL, AUTO, etc.)
 * @param createdBy Who created the snapshot (player UUID or "SYSTEM")
 * @param reason Optional reason for the snapshot
 * @param serializer The serializer to use for encoding inventory data
 * @param config Configuration for what to include in the snapshot
 * @return CompletableFuture with the created snapshot profile
 */
 public CompletableFuture<InventoryProfile> createSnapshotAsync(
 Player player,
 String name,
 String type,
 String createdBy,
 String reason,
 InventorySerializer serializer,
 InventoryConfig config
 ) {
 if (player == null || serializer == null) return CompletableFuture.completedFuture(null);

 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 // Capture player state
 InventorySnapshot snapshot = capturePlayerState(player, config);

 // Build and populate the profile
 InventoryProfile profile = buildSnapshotProfile(
 player, name, type, createdBy, reason, snapshot, serializer
 );

 save(profile);
 CoreDatabase.getEntityManager().flush();

 return profile;
 }));
 }

 /**
 * Creates a snapshot from an existing InventorySnapshot object.
 * Useful when you already have the snapshot data captured.
 */
 public CompletableFuture<InventoryProfile> createSnapshotFromDataAsync(
 Player player,
 String name,
 String type,
 String createdBy,
 String reason,
 InventorySnapshot snapshot,
 InventorySerializer serializer
 ) {
 if (player == null || snapshot == null || serializer == null) {
 return CompletableFuture.completedFuture(null);
 }

 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 InventoryProfile profile = buildSnapshotProfile(
 player, name, type, createdBy, reason, snapshot, serializer
 );

 save(profile);
 CoreDatabase.getEntityManager().flush();

 return profile;
 }));
 }

 /**
 * Lists all snapshots for a player, sorted by creation date (newest first).
 */
 public CompletableFuture<List<InventoryProfile>> listSnapshotsAsync(UUID playerUuid) {
 if (playerUuid == null) return CompletableFuture.completedFuture(List.of());

 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 String uuidStr = playerUuid.toString();
 return findAll().stream()
 .filter(s -> s != null && uuidStr.equals(s.getPlayerUuid()) && s.isSnapshot())
 .sorted(Comparator.comparingLong(InventoryProfile::getCreatedAt).reversed())
 .toList();
 }));
 }

 /**
 * Gets a specific snapshot by ID.
 */
 public CompletableFuture<InventoryProfile> getSnapshotAsync(long id) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 InventoryProfile profile = findById(id);
 return profile != null && profile.isSnapshot() ? profile : null;
 }));
 }

 /**
 * Deletes a snapshot by ID.
 */
 public CompletableFuture<Boolean> deleteSnapshotAsync(long id) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 InventoryProfile snapshot = findById(id);
 if (snapshot != null && snapshot.isSnapshot()) {
 delete(snapshot);
 return true;
 }
 return false;
 }));
 }

 /**
 * Prunes old snapshots for a player based on count and age limits.
 */
 public CompletableFuture<Integer> pruneSnapshotsAsync(UUID playerUuid, int maxCount, long maxAgeMillis) {
 if (playerUuid == null) return CompletableFuture.completedFuture(0);

 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 String uuidStr = playerUuid.toString();
 long cutoffTime = System.currentTimeMillis() - maxAgeMillis;

 List<InventoryProfile> snapshots = findAll().stream()
 .filter(s -> s != null && uuidStr.equals(s.getPlayerUuid()) && s.isSnapshot())
 .sorted(Comparator.comparingLong(InventoryProfile::getCreatedAt).reversed())
 .toList();

 int deleted = 0;

 // Delete snapshots older than max age
 for (InventoryProfile snapshot : snapshots) {
 if (snapshot.getCreatedAt() < cutoffTime) {
 delete(snapshot);
 deleted++;
 }
 }

 // Delete excess snapshots beyond max count
 if (snapshots.size() - deleted > maxCount) {
 List<InventoryProfile> remaining = findAll().stream()
 .filter(s -> s != null && uuidStr.equals(s.getPlayerUuid()) && s.isSnapshot())
 .sorted(Comparator.comparingLong(InventoryProfile::getCreatedAt).reversed())
 .toList();

 for (int i = maxCount; i < remaining.size(); i++) {
 delete(remaining.get(i));
 deleted++;
 }
 }

 return deleted;
 }));
 }

 // ==================== Cache Management ====================

 public void invalidate(String playerUuid, String contextKey) {
 if (playerUuid == null || contextKey == null) return;
 cache.remove(cacheKey(playerUuid, contextKey));
 }

 public void invalidateAll() {
 cache.clear();
 }

 // ==================== Private Helpers ====================

 /**
 * Captures the current state of a player into an InventorySnapshot.
 */
 private InventorySnapshot capturePlayerState(Player player, InventoryConfig config) {
 PlayerInventory inv = player.getInventory();
 ItemStack[] storage = clone(inv.getStorageContents());
 ItemStack[] armor = clone(inv.getArmorContents());
 ItemStack[] extra = config.includeOffhand ? clone(inv.getExtraContents()) : new ItemStack[0];
 ItemStack[] ender = config.includeEnderChest ? clone(player.getEnderChest().getContents()) : new ItemStack[0];
 List<PotionEffect> effects = config.applyPotions ? player.getActivePotionEffects().stream().toList() : List.of();

 return new InventorySnapshot(
 storage,
 armor,
 extra,
 ender,
 effects,
 player.getHealth(),
 player.getFoodLevel(),
 player.getSaturation(),
 player.getLevel(),
 player.getExp(),
 inv.getHeldItemSlot()
 );
 }

 /**
 * Builds an InventoryProfile from snapshot data.
 */
 private InventoryProfile buildSnapshotProfile(
 Player player,
 String name,
 String type,
 String createdBy,
 String reason,
 InventorySnapshot snapshot,
 InventorySerializer serializer
 ) {
 InventoryProfile profile = new InventoryProfile();
 profile.setPlayerUuid(player.getUniqueId().toString());
 profile.setPlayerName(player.getName());
 profile.setSnapshotName(name != null ? name : "");
 profile.setSnapshotType(type != null ? type : "MANUAL");
 profile.setCreatedBy(createdBy != null ? createdBy : "SYSTEM");
 profile.setReason(reason != null ? reason : "");
 profile.setWorldGroup(player.getWorld().getName());
 profile.setGameMode(player.getGameMode().name());
 profile.setLocationWorld(player.getWorld().getName());
 profile.setLocationX(player.getLocation().getX());
 profile.setLocationY(player.getLocation().getY());
 profile.setLocationZ(player.getLocation().getZ());
 profile.setYaw(player.getLocation().getYaw());
 profile.setPitch(player.getLocation().getPitch());
 profile.setInventoryData(serializer.encodeItems(snapshot.storage()).getBytes());
 profile.setArmorData(serializer.encodeItems(snapshot.armor()).getBytes());
 profile.setExtraData(serializer.encodeItems(snapshot.extra()).getBytes());
 profile.setEnderChestData(serializer.encodeItems(snapshot.enderChest()).getBytes());
 profile.setPotionData(serializer.encodeEffects(snapshot.effects()).getBytes());
 profile.setHealth(snapshot.health());
 profile.setFoodLevel(snapshot.foodLevel());
 profile.setSaturation(snapshot.saturation());
 profile.setExpLevel(snapshot.expLevel());
 profile.setExpProgress(snapshot.expProgress());
 profile.setHeldSlot(snapshot.heldSlot());
 profile.setCreatedAt(System.currentTimeMillis());
 profile.setUpdatedAt(System.currentTimeMillis());
 return profile;
 }

 private InventoryProfile findCurrentProfile(String playerUuid, String contextKey) {
 return findAll().stream()
 .filter(profile -> profile != null
 && playerUuid.equals(profile.getPlayerUuid())
 && contextKey.equalsIgnoreCase(profile.getContextKey())
 && !profile.isSnapshot())
 .findFirst()
 .orElse(null);
 }

 private static String cacheKey(String playerUuid, String contextKey) {
 return playerUuid + "|" + contextKey;
 }

 private void registerSyncListenerIfAvailable() {
 if (syncRegistered) return;
 de.t14d3.rapunzelcore.database.sync.DbEntitySync sync = CoreDatabase.entitySync();
 if (sync == null) return;
 synchronized (this) {
 if (syncRegistered) return;
 sync.register(this::onCacheEvent);
 syncRegistered = true;
 }
 }

 private void onCacheEvent(CacheEvent event, String sourceServer) {
 if (event == null || event.key() == null) return;
 if (!InventoryProfile.class.getName().equals(event.key().entityClassName())) return;
 invalidateAll();
 }

 private static InventoryRecord toRecord(InventoryProfile profile) {
 if (profile == null) return null;
 InventoryContext context = new InventoryContext(profile.getWorldGroup(), profile.getGameMode());
 return new InventoryRecord(
 context,
 new String(profile.getInventoryData()),
 new String(profile.getArmorData()),
 new String(profile.getExtraData()),
 new String(profile.getEnderChestData()),
 new String(profile.getPotionData()),
 profile.getHealth(),
 profile.getFoodLevel(),
 profile.getSaturation(),
 profile.getExpLevel(),
 profile.getExpProgress(),
 profile.getHeldSlot(),
 profile.getUpdatedAt()
 );
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
}
