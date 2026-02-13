package de.t14d3.rapunzelcore.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t14d3.rapunzelcore.velocity.VelocityNetworkBridge;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener for entity transfer events on the Velocity proxy.
 *
 * This listener handles:
 * - ServerPreConnectEvent - Intercepts player connections to route entity transfers
 * - DisconnectEvent - Cleans up pending transfers when players disconnect
 *
 * The listener works with the VelocityNetworkBridge to track pending entity transfers
 * and ensure entities are properly routed to their target servers.
 *
 * Note: All entity transfer forwarding logic is centralized in VelocityNetworkBridge.
 * This listener only handles player connection events and delegates forwarding to the bridge.
 */
public class EntityTransferListener {

 private final ProxyServer proxy;
 private final Logger logger;
 private final VelocityNetworkBridge networkBridge;

 // Track players with pending entity transfers: playerUuid -> TransferInfo
 private final Map<UUID, PendingPlayerTransfer> pendingPlayerTransfers;

 /**
 * Information about a pending player transfer.
 */
 private record PendingPlayerTransfer(
 UUID playerUuid,
 UUID entityUuid,
 String sourceServer,
 String targetServer,
 long timestamp
 ) {}

 /**
 * Creates a new EntityTransferListener instance.
 *
 * @param proxy the Velocity proxy server
 * @param logger the logger instance
 * @param networkBridge the network bridge for forwarding messages
 */
 public EntityTransferListener(
 @NotNull ProxyServer proxy,
 @NotNull Logger logger,
 @NotNull VelocityNetworkBridge networkBridge) {
 this.proxy = proxy;
 this.logger = logger;
 this.networkBridge = networkBridge;
 this.pendingPlayerTransfers = new ConcurrentHashMap<>();
 }

 /**
 * Handles server pre-connect events to intercept and route entity transfers.
 *
 * When a player is about to connect to a server, this method checks if there
 * are any pending entity transfers associated with this player. If so, it ensures
 * the player is routed to the correct target server.
 *
 * @param event the server pre-connect event
 */
 @Subscribe
 public void onServerPreConnect(@NotNull ServerPreConnectEvent event) {
 Player player = event.getPlayer();
 UUID playerUuid = player.getUniqueId();

 // Check if this player has a pending entity transfer
 PendingPlayerTransfer pendingTransfer = pendingPlayerTransfers.get(playerUuid);
 if (pendingTransfer != null) {
 handlePendingPlayerTransfer(event, player, pendingTransfer);
 return;
 }

 // Check if the target server has any pending transfers for this player
 event.getResult().getServer().ifPresent(targetServer -> {
 String targetServerName = targetServer.getServerInfo().getName();

 // Check if there's a pending entity transfer targeting this server for this player
 if (networkBridge.hasPendingTransfer(playerUuid)) {
 logger.debug("Player {} connecting to target server '{}' with pending entity transfer",
 player.getUsername(), targetServerName);
 }
 });
 }

 /**
 * Handles a pending player transfer by validating and routing appropriately.
 *
 * @param event the server pre-connect event
 * @param player the player
 * @param pendingTransfer the pending transfer information
 */
 private void handlePendingPlayerTransfer(
 @NotNull ServerPreConnectEvent event,
 @NotNull Player player,
 @NotNull PendingPlayerTransfer pendingTransfer) {

 String targetServerName = pendingTransfer.targetServer();

 // Validate the target server exists
 RegisteredServer targetServer = proxy.getServer(targetServerName).orElse(null);
 if (targetServer == null) {
 logger.warn("Pending transfer for player {} targets non-existent server '{}'",
 player.getUsername(), targetServerName);
 pendingPlayerTransfers.remove(player.getUniqueId());
 return;
 }

 // Check if player is already going to the correct server
 event.getResult().getServer().ifPresent(currentTarget -> {
 String currentTargetName = currentTarget.getServerInfo().getName();

 if (!currentTargetName.equalsIgnoreCase(targetServerName)) {
 // Redirect to the correct server
 logger.info("Redirecting player {} to target server '{}' for entity transfer",
 player.getUsername(), targetServerName);
 event.setResult(ServerPreConnectEvent.ServerResult.allowed(targetServer));
 } else {
 logger.debug("Player {} already routed to correct server '{}'",
 player.getUsername(), targetServerName);
 }
 });

 // Remove from pending (transfer is being processed)
 pendingPlayerTransfers.remove(player.getUniqueId());
 }

 /**
 * Handles player disconnect events to clean up pending transfers.
 *
 * @param event the disconnect event
 */
 @Subscribe
 public void onPlayerDisconnect(@NotNull DisconnectEvent event) {
 Player player = event.getPlayer();
 UUID playerUuid = player.getUniqueId();

 // Clean up any pending transfers for this player
 PendingPlayerTransfer removed = pendingPlayerTransfers.remove(playerUuid);
 if (removed != null) {
 logger.debug("Cleaned up pending transfer for disconnected player {} (entity: {})",
 player.getUsername(), removed.entityUuid());
 }

 // Also clean up any pending transfers where this player is the entity
 if (networkBridge.hasPendingTransfer(playerUuid)) {
 networkBridge.completeTransfer(playerUuid, false);
 logger.debug("Cancelled pending entity transfer for disconnected player {}",
 player.getUsername());
 }
 }

 /**
 * Registers a pending player transfer for tracking.
 *
 * This is called when an entity transfer is initiated for a player-owned entity
 * (like a pet) that should follow the player to the target server.
 *
 * @param playerUuid the player's UUID
 * @param entityUuid the entity being transferred
 * @param sourceServer the source server name
 * @param targetServer the target server name
 */
 public void registerPendingPlayerTransfer(
 @NotNull UUID playerUuid,
 @NotNull UUID entityUuid,
 @NotNull String sourceServer,
 @NotNull String targetServer) {

 PendingPlayerTransfer transfer = new PendingPlayerTransfer(
 playerUuid,
 entityUuid,
 sourceServer,
 targetServer,
 System.currentTimeMillis()
 );

 pendingPlayerTransfers.put(playerUuid, transfer);

 logger.debug("Registered pending player transfer: player={}, entity={}, source='{}', target='{}'",
 playerUuid, entityUuid, sourceServer, targetServer);
 }

 /**
 * Gets the count of pending player transfers.
 *
 * @return the number of pending player transfers
 */
 public int getPendingPlayerTransferCount() {
 return pendingPlayerTransfers.size();
 }

 /**
 * Checks if a player has a pending transfer.
 *
 * @param playerUuid the player's UUID
 * @return true if the player has a pending transfer
 */
 public boolean hasPendingTransfer(@NotNull UUID playerUuid) {
 return pendingPlayerTransfers.containsKey(playerUuid);
 }

 /**
 * Clears all pending transfers (useful for shutdown).
 */
 public void clearAllPendingTransfers() {
 int count = pendingPlayerTransfers.size();
 pendingPlayerTransfers.clear();
 logger.debug("Cleared {} pending player transfers", count);
 }

 /**
 * Gets the VelocityNetworkBridge instance.
 *
 * @return the network bridge
 */
 public VelocityNetworkBridge getNetworkBridge() {
 return networkBridge;
 }
}
