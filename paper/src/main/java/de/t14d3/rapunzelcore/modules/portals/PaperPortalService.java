package de.t14d3.rapunzelcore.modules.portals;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.t14d3.rapunzelcore.MessageHandler;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.PortalEntity;
import de.t14d3.rapunzelcore.modules.portals.database.PortalRepository;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferRequest;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Paper implementation of PortalService with particle effects and cross-server support.
 * Simplified to use PortalAction list instead of separate target fields.
 */
public class PaperPortalService extends AbstractPortalTransferService implements PluginMessageListener {

 private static final String CHANNEL_PORTALS = "rapunzelcore:portals";
 private static final long SYNC_INTERVAL_TICKS = 6000L; // 5 minutes

 private final RapunzelPaperCore plugin;
 private final EntityTransferService entityTransferService;
 private final Set<UUID> entitiesInPortals = ConcurrentHashMap.newKeySet();
 private final String localServerName;
 private final boolean crossServerEnabled;
 private final MessageHandler messageHandler;

 private BukkitTask particleTask;
 private BukkitTask syncTask;
 private volatile boolean shutdown = false;

 public PaperPortalService(RapunzelPaperCore plugin, EntityTransferService entityTransferService) {
 super(plugin.getLogger(), plugin.getMessenger(), resolveLocalServerName(plugin), 
 plugin.getConfiguration().getBoolean("portals.cross-server-enabled", true), 
 plugin.getConfiguration().getLong("portals.cooldown-millis", 1000L));
 this.plugin = plugin;
 this.entityTransferService = entityTransferService;
 this.localServerName = resolveLocalServerName(plugin);
 this.crossServerEnabled = plugin.getConfiguration().getBoolean("portals.cross-server-enabled", true);
 this.messageHandler = new MessageHandler();

 // Register plugin messaging
 Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_PORTALS, this);
 Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_PORTALS);

 // Start tasks
 startParticleTask();
 startSyncTask();

 // Load portals
 loadPortals();
 }

 @Override
 public CompletableFuture<Portal> createPortal(@NotNull Portal portal) {
 return CompletableFuture.supplyAsync(() -> {
 try {
 PortalRepository repository = PortalRepository.getInstance();
 PortalEntity entity = new PortalEntity(portal);
 repository.save(entity, true);

 registeredPortals.put(portal.id(), portal);

 if (crossServerEnabled) {
 broadcastPortalUpdate(portal, "CREATE");
 }

 return portal;
 } catch (Exception e) {
 plugin.getLogger().log(Level.SEVERE, "Failed to create portal", e);
 throw new RuntimeException("Failed to create portal", e);
 }
 });
 }

 @Override
 public CompletableFuture<Portal> updatePortal(@NotNull Portal portal) {
 return CompletableFuture.supplyAsync(() -> {
 try {
 Portal updated = new Portal(
 portal.id(), portal.name(), portal.world(),
 portal.minX(), portal.minY(), portal.minZ(),
 portal.maxX(), portal.maxY(), portal.maxZ(),
 portal.targetServer(), portal.targetWorld(),
 portal.targetX(), portal.targetY(), portal.targetZ(),
 portal.targetYaw(), portal.targetPitch(),
 portal.type(), portal.actions(), portal.particleConfig(),
 portal.enabled(), portal.allowEntities(), portal.allowPlayers(),
 portal.permission(), portal.metadata(),
 portal.createdAt(), Instant.now()
 );

 PortalRepository repository = PortalRepository.getInstance();
 PortalEntity entity = new PortalEntity(updated);
 repository.save(entity, true);
 registeredPortals.put(updated.id(), updated);

 if (crossServerEnabled) {
 broadcastPortalUpdate(updated, "UPDATE");
 }

 return updated;
 } catch (Exception e) {
 plugin.getLogger().log(Level.SEVERE, "Failed to update portal", e);
 throw new RuntimeException("Failed to update portal", e);
 }
 });
 }

 @Override
 public CompletableFuture<Boolean> deletePortal(@NotNull UUID portalId) {
 return CompletableFuture.supplyAsync(() -> {
 try {
 PortalRepository repository = PortalRepository.getInstance();
 repository.deleteById(portalId);
 registeredPortals.remove(portalId);

 if (crossServerEnabled) {
 broadcastPortalDeletion(portalId);
 }

 return true;
 } catch (Exception e) {
 plugin.getLogger().log(Level.SEVERE, "Failed to delete portal", e);
 return false;
 }
 });
 }

 @Override
 public Optional<Portal> getPortal(@NotNull UUID portalId) {
 return Optional.ofNullable(registeredPortals.get(portalId));
 }

 @Override
 public Optional<Portal> getPortalByName(@NotNull String name) {
 return registeredPortals.values().stream()
 .filter(p -> p.name().equalsIgnoreCase(name))
 .findFirst();
 }

 @Override
 public Collection<Portal> getAllPortals() {
 return List.copyOf(registeredPortals.values());
 }

 @Override
 public Collection<Portal> getPortalsInWorld(@NotNull String world) {
 return registeredPortals.values().stream()
 .filter(p -> p.world().equals(world))
 .toList();
 }

 @Override
 public CompletableFuture<Boolean> enablePortal(@NotNull UUID portalId) {
 Portal portal = registeredPortals.get(portalId);
 if (portal == null) {
 return CompletableFuture.completedFuture(false);
 }
 return updatePortal(new Portal(
 portal.id(), portal.name(), portal.world(),
 portal.minX(), portal.minY(), portal.minZ(),
 portal.maxX(), portal.maxY(), portal.maxZ(),
 portal.targetServer(), portal.targetWorld(),
 portal.targetX(), portal.targetY(), portal.targetZ(),
 portal.targetYaw(), portal.targetPitch(),
 portal.type(), portal.actions(), portal.particleConfig(),
 true, portal.allowEntities(), portal.allowPlayers(),
 portal.permission(), portal.metadata(),
 portal.createdAt(), Instant.now()
 )).thenApply(p -> true);
 }

 @Override
 public CompletableFuture<Boolean> disablePortal(@NotNull UUID portalId) {
 Portal portal = registeredPortals.get(portalId);
 if (portal == null) {
 return CompletableFuture.completedFuture(false);
 }
 return updatePortal(new Portal(
 portal.id(), portal.name(), portal.world(),
 portal.minX(), portal.minY(), portal.minZ(),
 portal.maxX(), portal.maxY(), portal.maxZ(),
 portal.targetServer(), portal.targetWorld(),
 portal.targetX(), portal.targetY(), portal.targetZ(),
 portal.targetYaw(), portal.targetPitch(),
 portal.type(), portal.actions(), portal.particleConfig(),
 false, portal.allowEntities(), portal.allowPlayers(),
 portal.permission(), portal.metadata(),
 portal.createdAt(), Instant.now()
 )).thenApply(p -> true);
 }

 @Override
 public boolean canUsePortal(@NotNull Portal portal, @NotNull UUID playerId) {
 if (portal.permission() == null || portal.permission().isEmpty()) {
 return true;
 }
 Player player = Bukkit.getPlayer(playerId);
 return player != null && player.hasPermission(portal.permission());
 }

 @Override
 public CompletableFuture<Void> processPortalEntry(@NotNull UUID entityId, @NotNull Portal portal) {
 return CompletableFuture.runAsync(() -> {
 long now = System.currentTimeMillis();
 Long cooldown = entityPortalCooldowns.get(entityId);
 if (cooldown != null && now - cooldown < getPortalCooldownMillis()) {
 return;
 }
 entityPortalCooldowns.put(entityId, now);

 Entity entity = Bukkit.getEntity(entityId);
 if (entity == null || !entity.isValid()) {
 return;
 }

 if (entity instanceof Player player) {
 if (!portal.allowPlayers()) {
 return;
 }
 if (!canUsePortal(portal, entityId)) {
 player.sendMessage(messageHandler.getMessage("portal.no-permission"));
 return;
 }
 } else if (!portal.allowEntities()) {
 return;
 }

 switch (portal.type()) {
 case TELEPORT -> handleTeleport(entity, portal);
 case COMMAND -> handleCommand(entity, portal);
 default -> handleDefault(entity, portal);
 }
 });
 }

 @Override
 public void registerActionHandler(@NotNull String actionType, @NotNull PortalActionHandler handler) {
 // Action handlers are now handled via PortalAction sealed interface
 }

 @Override
 public void unregisterActionHandler(@NotNull String actionType) {
 // Action handlers are now handled via PortalAction sealed interface
 }

 @Override
 @NotNull
 public String getLocalServerName() {
 return localServerName;
 }

 @Override
 public boolean isCrossServerEnabled() {
 return crossServerEnabled;
 }

 @Override
 public CompletableFuture<Void> reloadPortals() {
 return CompletableFuture.runAsync(() -> {
 registeredPortals.clear();
 loadPortals();
 });
 }

 @Override
 public CompletableFuture<TransferResult> transferEntity(UUID entityUuid, String targetServer, 
 String targetWorld, double x, double y, double z, float yaw, float pitch) {
 if (entityTransferService == null) {
 return CompletableFuture.completedFuture(
 new TransferResult(false, entityUuid, targetServer, "Entity transfer service not available")
 );
 }
 return entityTransferService.transferEntity(entityUuid, targetServer, targetWorld, x, y, z, yaw, pitch);
 }

 @Override
 public CompletableFuture<UUID> receiveEntity(EntityTransferRequest request) {
 if (entityTransferService == null) {
 return CompletableFuture.completedFuture(null);
 }
 return entityTransferService.receiveEntity(request);
 }

 @Override
 public CompletableFuture<SerializedEntity> serializeEntity(UUID entityUuid) {
 if (entityTransferService == null) {
 return CompletableFuture.completedFuture(null);
 }
 return entityTransferService.serializeEntity(entityUuid);
 }

 @Override
 public boolean isTransferEnabled() {
 return crossServerEnabled && entityTransferService != null;
 }

 @Override
 protected void firePortalEntryEvent(PortalEntryEvent event) {
 Bukkit.getPluginManager().callEvent(new PaperPortalEntryEvent(event));
 }

 @Override
 protected CompletableFuture<Void> executeLocalTeleport(UUID entityUuid, Portal portal) {
 return CompletableFuture.runAsync(() -> {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity == null || !entity.isValid()) {
 return;
 }

 Bukkit.getScheduler().runTask(plugin, () -> {
 World targetWorld = portal.targetWorld() != null 
 ? Bukkit.getWorld(portal.targetWorld()) 
 : Bukkit.getWorld(portal.world());

 if (targetWorld == null) return;

 Location target = new Location(targetWorld, portal.targetX(), portal.targetY(), 
 portal.targetZ(), portal.targetYaw(), portal.targetPitch());

 entity.teleport(target);
 });
 });
 }

 @Override
 protected void executeTeleportAction(UUID entityUuid, Portal portal, PortalAction.TeleportAction action) {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity == null || !entity.isValid()) {
 return;
 }

 if (action.targetServer() != null && !action.targetServer().isEmpty()) {
 if (!isTransferEnabled()) {
 if (entity instanceof Player player) {
 player.sendMessage(messageHandler.getMessage("portal.cross-server-disabled"));
 }
 return;
 }

 transferEntity(entityUuid, action.targetServer(), action.targetWorld(),
 action.targetX(), action.targetY(), action.targetZ(),
 action.targetYaw(), action.targetPitch()).thenAccept(result -> {
 Bukkit.getScheduler().runTask(plugin, () -> {
 if (!result.success() && entity instanceof Player player) {
 result.errorMessage().ifPresent(msg -> 
 player.sendMessage(messageHandler.getMessage("portal.transfer-failed", msg)));
 }
 });
 });
 } else {
 Bukkit.getScheduler().runTask(plugin, () -> {
 World targetWorld = Bukkit.getWorld(action.targetWorld());
 if (targetWorld == null) return;

 Location target = new Location(targetWorld, action.targetX(), action.targetY(),
 action.targetZ(), action.targetYaw(), action.targetPitch());

 entity.teleport(target);
 });
 }
 }

 @Override
 protected void executeCommandAction(UUID entityUuid, Portal portal, PortalAction.CommandAction action) {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity == null || !entity.isValid()) {
 return;
 }

 List<String> commands = action.commands();
 boolean executeAsConsole = action.executeAsConsole();
 boolean executeAsEntity = action.executeAsEntity();

 for (String command : commands) {
 String processedCommand = command
 .replace("{player}", entity.getName())
 .replace("{uuid}", entity.getUniqueId().toString())
 .replace("{portal}", portal.name());

 if (executeAsEntity && entity instanceof Player player) {
 Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(processedCommand));
 } else if (executeAsConsole) {
 Bukkit.getScheduler().runTask(plugin, () -> 
 Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand));
 }
 }
 }

 @Override
 protected void removeLocalEntity(UUID entityUuid) {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity != null && entity.isValid()) {
 Bukkit.getScheduler().runTask(plugin, entity::remove);
 }
 }

 @Override
 protected UUID getEntityOwner(UUID entityUuid) {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity instanceof Player player) {
 return player.getUniqueId();
 }
 return null;
 }

 @Override
 protected String getEntityType(UUID entityUuid) {
 Entity entity = Bukkit.getEntity(entityUuid);
 return entity != null ? entity.getType().name() : null;
 }

 @Override
 protected Map<String, Object> getEntityMetadata(UUID entityUuid) {
 Entity entity = Bukkit.getEntity(entityUuid);
 if (entity == null) {
 return Map.of();
 }
 Map<String, Object> metadata = new HashMap<>();
 metadata.put("type", entity.getType().name());
 metadata.put("name", entity.getName());
 metadata.put("customName", entity.customName() != null ? entity.customName() : entity.getName());
 return metadata;
 }

 @Override
 public void spawnPortalParticles(@NotNull Portal portal) {
 if (portal.particleConfig() == null || !portal.particleConfig().enabled()) {
 return;
 }

 World world = Bukkit.getWorld(portal.world());
 if (world == null) {
 return;
 }

 ParticleConfig config = portal.particleConfig();
 double[] center = portal.getCenter();

 try {
 Particle particle = Particle.valueOf(config.particleType());
 world.spawnParticle(particle, center[0], center[1], center[2],
 config.count(), config.offsetX(), config.offsetY(), config.offsetZ(), config.speed());
 } catch (IllegalArgumentException e) {
 // Invalid particle type, skip
 }
 }

 @Override
 public void shutdown() {
 shutdown = true;

 if (particleTask != null) {
 particleTask.cancel();
 }
 if (syncTask != null) {
 syncTask.cancel();
 }

 Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_PORTALS);
 Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_PORTALS);

 registeredPortals.clear();
 }

 @Override
 public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
 if (!channel.equals(CHANNEL_PORTALS) || !crossServerEnabled) {
 return;
 }

 try {
 ByteArrayDataInput input = ByteStreams.newDataInput(message);
 String action = input.readUTF();

 switch (action) {
 case "PORTAL_CREATE", "PORTAL_UPDATE" -> handleRemotePortalUpdate(input);
 case "PORTAL_DELETE" -> handleRemotePortalDelete(input);
 case "PORTAL_SYNC_REQUEST" -> handleSyncRequest(input);
 }
 } catch (Exception e) {
 plugin.getLogger().log(Level.WARNING, "Failed to process portal message", e);
 }
 }

 private void loadPortals() {
 try {
 PortalRepository repository = PortalRepository.getInstance();
 List<Portal> loaded = repository.snapshotById().values().stream()
 .map(PortalEntity::toPortal)
 .toList();
 for (Portal portal : loaded) {
 registeredPortals.put(portal.id(), portal);
 }
 plugin.getLogger().info("Loaded " + loaded.size() + " portals");
 } catch (Exception e) {
 plugin.getLogger().log(Level.SEVERE, "Failed to load portals", e);
 }
 }

 private void startParticleTask() {
 particleTask = new BukkitRunnable() {
 @Override
 public void run() {
 if (shutdown) {
 cancel();
 return;
 }
 spawnParticles();
 }
 }.runTaskTimer(plugin, 5L, 5L);
 }

 private void startSyncTask() {
 if (!crossServerEnabled) return;

 syncTask = new BukkitRunnable() {
 @Override
 public void run() {
 if (shutdown) {
 cancel();
 return;
 }
 requestPortalSync();
 }
 }.runTaskTimer(plugin, SYNC_INTERVAL_TICKS, SYNC_INTERVAL_TICKS);
 }

 private void spawnParticles() {
 for (Portal portal : registeredPortals.values()) {
 if (!portal.enabled() || portal.particleConfig() == null || !portal.particleConfig().enabled()) {
 continue;
 }
 spawnPortalParticles(portal);
 }
 }

 private void handleTeleport(Entity entity, Portal portal) {
 for (PortalAction action : portal.actions()) {
 if (action instanceof PortalAction.TeleportAction teleportAction) {
 executeTeleportAction(entity.getUniqueId(), portal, teleportAction);
 } else if (action instanceof PortalAction.CommandAction commandAction) {
 executeCommandAction(entity.getUniqueId(), portal, commandAction);
 }
 }

 if (portal.actions().isEmpty() && portal.targetWorld() != null) {
 Bukkit.getScheduler().runTask(plugin, () -> {
 World targetWorld = portal.targetWorld() != null 
 ? Bukkit.getWorld(portal.targetWorld()) 
 : Bukkit.getWorld(portal.world());

 if (targetWorld == null) return;

 Location target = new Location(targetWorld, portal.targetX(), portal.targetY(),
 portal.targetZ(), portal.targetYaw(), portal.targetPitch());

 entity.teleport(target);
 });
 }
 }

 private void handleCommand(Entity entity, Portal portal) {
 for (PortalAction action : portal.actions()) {
 if (action instanceof PortalAction.CommandAction commandAction) {
 executeCommandAction(entity.getUniqueId(), portal, commandAction);
 }
 }
 }

 private void handleDefault(Entity entity, Portal portal) {
 handleTeleport(entity, portal);
 }

 private void broadcastPortalUpdate(Portal portal, String action) {
 if (!crossServerEnabled) return;

 try {
 ByteArrayDataOutput output = ByteStreams.newDataOutput();
 output.writeUTF("PORTAL_" + action);
 output.writeUTF(portal.id().toString());
 output.writeUTF(portal.name());
 output.writeUTF(portal.world());

 plugin.getMessenger().sendToAll(CHANNEL_PORTALS, new String(output.toByteArray()));
 } catch (Exception e) {
 plugin.getLogger().log(Level.WARNING, "Failed to broadcast portal update", e);
 }
 }

 private void broadcastPortalDeletion(UUID portalId) {
 if (!crossServerEnabled) return;

 try {
 ByteArrayDataOutput output = ByteStreams.newDataOutput();
 output.writeUTF("PORTAL_DELETE");
 output.writeUTF(portalId.toString());

 plugin.getMessenger().sendToAll(CHANNEL_PORTALS, new String(output.toByteArray()));
 } catch (Exception e) {
 plugin.getLogger().log(Level.WARNING, "Failed to broadcast portal deletion", e);
 }
 }

 private void handleRemotePortalUpdate(ByteArrayDataInput input) {
 // Handle portal updates from other servers
 }

 private void handleRemotePortalDelete(ByteArrayDataInput input) {
 UUID portalId = UUID.fromString(input.readUTF());
 registeredPortals.remove(portalId);
 }

 private void handleSyncRequest(ByteArrayDataInput input) {
 // Respond with local portal data
 }

 private void requestPortalSync() {
 try {
 ByteArrayDataOutput output = ByteStreams.newDataOutput();
 output.writeUTF("PORTAL_SYNC_REQUEST");
 output.writeUTF(localServerName);

 plugin.getMessenger().sendToAll(CHANNEL_PORTALS, new String(output.toByteArray()));
 } catch (Exception e) {
 plugin.getLogger().log(Level.WARNING, "Failed to request portal sync", e);
 }
 }

 private static String resolveLocalServerName(RapunzelPaperCore plugin) {
 String serverName = plugin.getConfiguration().getString("server-name", null);
 if (serverName != null && !serverName.isBlank()) {
 return serverName;
 }
 return Bukkit.getServer().getName();
 }

 void onEntityMove(Entity entity, Location from, Location to) {
 if (shutdown) return;

 Optional<Portal> portalOpt = findPortalAt(to.getWorld().getName(), to.getX(), to.getY(), to.getZ());

 if (portalOpt.isPresent()) {
 Portal portal = portalOpt.get();
 if (!entitiesInPortals.contains(entity.getUniqueId())) {
 entitiesInPortals.add(entity.getUniqueId());
 handlePortalEntry(entity.getUniqueId(), portal);
 }
 } else {
 entitiesInPortals.remove(entity.getUniqueId());
 }
 }
}
