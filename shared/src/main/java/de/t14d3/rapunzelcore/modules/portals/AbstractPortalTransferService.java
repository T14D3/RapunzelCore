package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.network.transfer.AbstractEntityTransferService;
import de.t14d3.rapunzellib.network.Messenger;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base implementation of PortalTransferService.
 * Provides common functionality for portal management, cooldown handling,
 * and cross-server entity transfers through portals.
 * Simplified to use PortalAction list instead of separate target fields.
 */
public abstract class AbstractPortalTransferService extends AbstractEntityTransferService
 implements PortalTransferService {

 protected final Map<UUID, Portal> registeredPortals = new ConcurrentHashMap<>();
 protected final Map<UUID, Long> entityPortalCooldowns = new ConcurrentHashMap<>();
 protected final long portalCooldownMillis;

 /**
 * Creates a new AbstractPortalTransferService.
 *
 * @param logger the logger instance
 * @param messenger the messenger for network communication
 * @param localServerName the name of this server in the network
 * @param transferEnabled whether entity transfer is enabled
 * @param portalCooldownMillis cooldown duration in milliseconds between portal uses
 */
 protected AbstractPortalTransferService(
 Logger logger,
 Messenger messenger,
 String localServerName,
 boolean transferEnabled,
 long portalCooldownMillis
 ) {
 super(logger, messenger, localServerName, transferEnabled);
 this.portalCooldownMillis = portalCooldownMillis;
 }

 /**
 * Creates a new AbstractPortalTransferService with default 1-second cooldown.
 *
 * @param logger the logger instance
 * @param messenger the messenger for network communication
 * @param localServerName the name of this server in the network
 * @param transferEnabled whether entity transfer is enabled
 */
 protected AbstractPortalTransferService(
 Logger logger,
 Messenger messenger,
 String localServerName,
 boolean transferEnabled
 ) {
 this(logger, messenger, localServerName, transferEnabled, 1000L);
 }

 @Override
 public CompletableFuture<Boolean> transferThroughPortal(@NotNull UUID entityUuid, @NotNull Portal portal) {
 if (!portal.enabled()) {
 logger.fine("Portal " + portal.id() + " is disabled, skipping transfer");
 return CompletableFuture.completedFuture(false);
 }

 // Check if this is a cross-server transfer
 if (portal.isCrossServer()) {
 if (!isTransferEnabled()) {
 logger.warning("Cross-server transfer requested but entity transfer is disabled");
 return CompletableFuture.completedFuture(false);
 }

 String targetServer = portal.targetServer();
 logger.info("Initiating cross-server portal transfer: " + entityUuid +
 " to server " + targetServer + " via portal " + portal.id());

 return transferEntity(
 entityUuid,
 targetServer,
 portal.targetWorld(),
 portal.targetX(),
 portal.targetY(),
 portal.targetZ(),
 portal.targetYaw(),
 portal.targetPitch()
 ).thenApply(TransferResult::success);
 } else {
 // Local teleportation
 logger.fine("Executing local portal teleport: " + entityUuid + " via portal " + portal.id());
 return executeLocalTeleport(entityUuid, portal)
 .thenApply(v -> true)
 .exceptionally(ex -> {
 logger.log(Level.SEVERE, "Failed to execute local teleport", ex);
 return false;
 });
 }
 }

 @Override
 public void handlePortalEntry(@NotNull UUID entityUuid, @NotNull Portal portal) {
 // Check cooldown
 if (isOnCooldown(entityUuid)) {
 long remaining = getRemainingCooldown(entityUuid);
 logger.fine("Entity " + entityUuid + " is on portal cooldown for " + remaining + "ms");
 return;
 }

 // Fire PortalEntryEvent
 boolean isCrossServer = portal.isCrossServer();
 PortalEntryEvent event = new PortalEntryEvent(entityUuid, portal, isCrossServer);

 // Call event (platform-specific implementation should fire this through the event bus)
 firePortalEntryEvent(event);

 if (event.isCancelled()) {
 logger.fine("PortalEntryEvent was cancelled for entity " + entityUuid);
 return;
 }

 // Set cooldown
 setCooldown(entityUuid);

 // Execute portal actions based on type
 switch (portal.type()) {
 case TELEPORT -> {
 // Execute teleport actions from the actions list
 for (PortalAction action : portal.actions()) {
 if (action instanceof PortalAction.TeleportAction teleportAction) {
 executeTeleportAction(entityUuid, portal, teleportAction);
 } else if (action instanceof PortalAction.CommandAction commandAction) {
 executeCommandAction(entityUuid, portal, commandAction);
 }
 }

 // Also handle legacy target fields if no actions defined
 if (portal.actions().isEmpty() && portal.targetWorld() != null) {
 transferThroughPortal(entityUuid, portal)
 .whenComplete((success, error) -> {
 if (error != null) {
 logger.log(Level.SEVERE, "Portal transfer failed for " + entityUuid, error);
 } else if (!success) {
 logger.warning("Portal transfer was not successful for " + entityUuid);
 }
 });
 }
 }
 case COMMAND -> {
 // Execute command actions
 for (PortalAction action : portal.actions()) {
 if (action instanceof PortalAction.CommandAction commandAction) {
 executeCommandAction(entityUuid, portal, commandAction);
 }
 }
 }
 default -> logger.warning("Unknown portal type: " + portal.type());
 }
 }

 /**
 * Executes a teleport action for an entity.
 *
 * @param entityUuid the entity to teleport
 * @param portal the portal
 * @param action the teleport action
 */
 protected abstract void executeTeleportAction(UUID entityUuid, Portal portal, PortalAction.TeleportAction action);

 /**
 * Executes a command action for an entity.
 *
 * @param entityUuid the entity
 * @param portal the portal
 * @param action the command action
 */
 protected abstract void executeCommandAction(UUID entityUuid, Portal portal, PortalAction.CommandAction action);

 @Override
 public void registerPortal(@NotNull Portal portal) {
 registeredPortals.put(portal.id(), portal);
 logger.fine("Registered portal: " + portal.id() + " (" + portal.name() + ")");
 }

 @Override
 public void unregisterPortal(@NotNull UUID portalId) {
 Portal removed = registeredPortals.remove(portalId);
 if (removed != null) {
 logger.fine("Unregistered portal: " + portalId + " (" + removed.name() + ")");
 }
 }

 @Override
 public Optional<Portal> findPortalAt(@NotNull String world, double x, double y, double z) {
 for (Portal portal : registeredPortals.values()) {
 if (portal.enabled() && portal.contains(world, x, y, z)) {
 return Optional.of(portal);
 }
 }
 return Optional.empty();
 }

 @Override
 public boolean isOnCooldown(@NotNull UUID entityUuid) {
 Long cooldownEnd = entityPortalCooldowns.get(entityUuid);
 if (cooldownEnd == null) {
 return false;
 }
 if (System.currentTimeMillis() >= cooldownEnd) {
 entityPortalCooldowns.remove(entityUuid);
 return false;
 }
 return true;
 }

 @Override
 public long getRemainingCooldown(@NotNull UUID entityUuid) {
 Long cooldownEnd = entityPortalCooldowns.get(entityUuid);
 if (cooldownEnd == null) {
 return 0;
 }
 long remaining = cooldownEnd - System.currentTimeMillis();
 if (remaining <= 0) {
 entityPortalCooldowns.remove(entityUuid);
 return 0;
 }
 return remaining;
 }

 @Override
 public long getPortalCooldownMillis() {
 return portalCooldownMillis;
 }

 /**
 * Sets the cooldown for an entity.
 *
 * @param entityUuid the entity UUID
 */
 protected void setCooldown(@NotNull UUID entityUuid) {
 entityPortalCooldowns.put(entityUuid, System.currentTimeMillis() + portalCooldownMillis);
 }

 /**
 * Fires the PortalEntryEvent through the platform's event system.
 * Platform-specific implementations should override this to dispatch the event.
 *
 * @param event the event to fire
 */
 protected abstract void firePortalEntryEvent(PortalEntryEvent event);

 /**
 * Executes a local teleport for an entity through a portal.
 * Platform-specific implementations should handle the actual teleportation.
 *
 * @param entityUuid the entity to teleport
 * @param portal the portal defining the destination
 * @return CompletableFuture that completes when teleport is done
 */
 protected abstract CompletableFuture<Void> executeLocalTeleport(UUID entityUuid, Portal portal);

 /**
 * Broadcasts a portal update to the network.
 *
 * @param portal the portal to broadcast
 */
 protected void broadcastPortalUpdate(Portal portal) {
 if (!isTransferEnabled()) {
 return;
 }

 // Send portal sync message
 try {
 eventBus.sendToAll(NetworkChannels.PORTALS, portal);
 logger.fine("Broadcasted portal update: " + portal.id());
 } catch (Exception e) {
 logger.log(Level.WARNING, "Failed to broadcast portal update", e);
 }
 }

 /**
 * Sends a portal entry notification to the target server.
 *
 * @param entityUuid the entity entering the portal
 * @param portal the portal being entered
 * @param targetServer the target server name
 */
 protected void sendPortalEntryNotification(UUID entityUuid, Portal portal, String targetServer) {
 // Create and send entry notification
 PortalEntryNotification notification = new PortalEntryNotification(
 entityUuid,
 portal.id(),
 localServerName,
 targetServer
 );

 try {
 eventBus.sendToServer(NetworkChannels.PORTALS, targetServer, notification);
 } catch (Exception e) {
 logger.log(Level.WARNING, "Failed to send portal entry notification", e);
 }
 }

 /**
 * Record for portal entry notifications sent to target servers.
 */
 protected record PortalEntryNotification(
 UUID entityUuid,
 UUID portalId,
 String sourceServer,
 String targetServer
 ) {}
}
