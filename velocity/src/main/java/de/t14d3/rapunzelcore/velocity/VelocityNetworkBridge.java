package de.t14d3.rapunzelcore.velocity;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferPayload;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Network bridge for forwarding messages between backend servers in a Velocity proxy environment.
 *
 * This class provides centralized methods to forward entity transfer data, portal sync messages,
 * and other cross-server communication. It tracks pending transfers and confirms completion.
 *
 * Key features:
 * - Forward messages to specific servers via registered server connections
 * - Broadcast messages to all registered backend servers
 * - Send confirmations back to originating servers
 * - Track pending transfers with timeouts and cleanup
 * - Centralized entity transfer forwarding logic
 */
public class VelocityNetworkBridge {

 private static final String PROXY_CHANNEL = "rapunzelcore:proxy";
 private static final long PENDING_TRANSFER_TIMEOUT_MS = 30000; // 30 seconds
 private static final long CLEANUP_INTERVAL_MS = 60000; // 60 seconds

 private final ProxyServer proxy;
 private final Logger logger;
 private final Gson gson;
 private final ChannelIdentifier channelIdentifier;

 // Track pending transfers: transferId -> PendingTransferInfo
 private final Map<UUID, PendingTransferInfo> pendingTransfers;

 // Track transfer confirmations: originalEntityId -> CompletableFuture<Confirmation>
 private final Map<UUID, CompletableFuture<TransferConfirmation>> confirmationFutures;

 /**
 * Information about a pending transfer for tracking purposes.
 */
 private record PendingTransferInfo(
 UUID transferId,
 UUID entityId,
 String sourceServer,
 String targetServer,
 long timestamp,
 CompletableFuture<Boolean> completionFuture
 ) {}

 /**
 * Transfer confirmation result.
 */
 public record TransferConfirmation(
 UUID originalEntityUuid,
 UUID newEntityUuid,
 boolean success,
 String message
 ) {}

 /**
 * Creates a new VelocityNetworkBridge instance.
 *
 * @param proxy the Velocity proxy server
 * @param logger the logger instance
 */
 public VelocityNetworkBridge(@NotNull ProxyServer proxy, @NotNull Logger logger) {
 this.proxy = proxy;
 this.logger = logger;
 this.gson = JsonCodecs.gson();
 this.channelIdentifier = MinecraftChannelIdentifier.from(PROXY_CHANNEL);
 this.pendingTransfers = new ConcurrentHashMap<>();
 this.confirmationFutures = new ConcurrentHashMap<>();

 // Start cleanup task
 startCleanupTask();
 }

 /**
 * Forwards an entity transfer to the target server.
 *
 * This is the centralized method for forwarding entity transfers from Velocity.
 * It handles serialization, forwarding, and pending transfer registration.
 *
 * @param payload the entity transfer payload
 * @param gson the Gson instance for serialization
 * @return true if the transfer was forwarded successfully
 */
 public boolean forwardEntityTransfer(@NotNull EntityTransferPayload payload, @NotNull Gson gson) {
 String targetServer = payload.targetServer();
 String jsonPayload = gson.toJson(payload);

 // Forward to target server
 boolean forwarded = forwardToServer(NetworkChannels.ENTITY_TRANSFER, targetServer, jsonPayload);

 if (forwarded) {
 // Register pending transfer for tracking
 registerPendingTransfer(
 payload.entityUuid(),
 payload.sourceServer(),
 targetServer
 );
 }

 return forwarded;
 }

 /**
 * Sends a transfer confirmation back to the originating server.
 *
 * @param originalEntityUuid the UUID of the original entity
 * @param newEntityUuid the UUID of the newly spawned entity (can be same as original)
 * @param sourceServer the server to send confirmation to
 * @param success whether the transfer was successful
 * @param message additional message or error description
 * @return true if confirmation was sent successfully
 */
 public boolean sendTransferConfirmation(
 @NotNull UUID originalEntityUuid,
 @Nullable UUID newEntityUuid,
 @NotNull String sourceServer,
 boolean success,
 @Nullable String message) {

 TransferConfirmation confirmation = new TransferConfirmation(
 originalEntityUuid,
 newEntityUuid != null ? newEntityUuid : originalEntityUuid,
 success,
 message != null ? message : ""
 );

 String payload = gson.toJson(confirmation);
 boolean sent = forwardToServer(NetworkChannels.ENTITY_TRANSFER, sourceServer, payload);

 if (sent) {
 logger.debug("Sent transfer confirmation to '{}' for entity {}", sourceServer, originalEntityUuid);
 } else {
 logger.warn("Failed to send transfer confirmation to '{}' for entity {}", sourceServer, originalEntityUuid);
 }

 // Complete any waiting future
 CompletableFuture<TransferConfirmation> future = confirmationFutures.remove(originalEntityUuid);
 if (future != null) {
 future.complete(confirmation);
 }

 return sent;
 }

 /**
 * Forwards a message to a specific backend server.
 *
 * @param channel the network channel (e.g., ENTITY_TRANSFER, PORTALS, PETS)
 * @param targetServer the name of the target server
 * @param payload the message payload as JSON string
 * @return true if the message was forwarded successfully, false otherwise
 */
 public boolean forwardToServer(@NotNull String channel, @NotNull String targetServer, @NotNull String payload) {
 Optional<RegisteredServer> targetOpt = proxy.getServer(targetServer);
 if (targetOpt.isEmpty()) {
 logger.warn("Cannot forward message to server '{}': server not found", targetServer);
 return false;
 }

 RegisteredServer target = targetOpt.get();

 // Find a player on the target server to use as message carrier
 Optional<com.velocitypowered.api.proxy.Player> carrier = proxy.getAllPlayers().stream()
 .filter(p -> p.getCurrentServer()
 .map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(targetServer))
 .orElse(false))
 .findFirst();

 if (carrier.isEmpty()) {
 logger.debug("No player available on target server '{}' to carry message", targetServer);
 return false;
 }

 Optional<ServerConnection> connection = carrier.get().getCurrentServer();
 if (connection.isEmpty()) {
 logger.debug("No active connection for player on server '{}'", targetServer);
 return false;
 }

 // Create wrapped message with channel info
 ProxyMessage message = new ProxyMessage(channel, payload, System.currentTimeMillis());
 String json = gson.toJson(message);
 byte[] data = json.getBytes(StandardCharsets.UTF_8);

 try {
 connection.get().sendPluginMessage(channelIdentifier, data);
 logger.debug("Forwarded message on channel '{}' to server '{}'", channel, targetServer);
 return true;
 } catch (Exception e) {
 logger.error("Failed to forward message to server '{}': {}", targetServer, e.getMessage());
 return false;
 }
 }

 /**
 * Broadcasts a message to all registered backend servers.
 *
 * @param channel the network channel
 * @param payload the message payload as JSON string
 * @param excludeServer optional server name to exclude from broadcast
 * @return the number of servers the message was successfully sent to
 */
 public int broadcastToAll(@NotNull String channel, @NotNull String payload, @Nullable String excludeServer) {
 int successCount = 0;

 for (RegisteredServer server : proxy.getAllServers()) {
 String serverName = server.getServerInfo().getName();

 if (excludeServer != null && excludeServer.equalsIgnoreCase(serverName)) {
 continue;
 }

 if (forwardToServer(channel, serverName, payload)) {
 successCount++;
 }
 }

 logger.debug("Broadcast message on channel '{}' to {} servers", channel, successCount);
 return successCount;
 }

 /**
 * Registers a pending transfer for tracking.
 *
 * @param entityId the entity being transferred
 * @param sourceServer the source server name
 * @param targetServer the target server name
 * @return a CompletableFuture that will complete when the transfer is confirmed
 */
 @NotNull
 public CompletableFuture<Boolean> registerPendingTransfer(
 @NotNull UUID entityId,
 @NotNull String sourceServer,
 @NotNull String targetServer) {

 UUID transferId = UUID.randomUUID();
 CompletableFuture<Boolean> future = new CompletableFuture<>();

 PendingTransferInfo info = new PendingTransferInfo(
 transferId,
 entityId,
 sourceServer,
 targetServer,
 System.currentTimeMillis(),
 future
 );

 pendingTransfers.put(transferId, info);
 pendingTransfers.put(entityId, info); // Also index by entity ID for quick lookup

 logger.debug("Registered pending transfer {} for entity {} from '{}' to '{}'",
 transferId, entityId, sourceServer, targetServer);

 // Set timeout
 future.orTimeout(PENDING_TRANSFER_TIMEOUT_MS / 1000, TimeUnit.SECONDS)
 .whenComplete((result, ex) -> {
 if (ex != null) {
 logger.warn("Transfer {} for entity {} timed out", transferId, entityId);
 pendingTransfers.remove(transferId);
 pendingTransfers.remove(entityId);
 }
 });

 return future;
 }

 /**
 * Completes a pending transfer.
 *
 * @param entityId the entity ID
 * @param success whether the transfer succeeded
 */
 public void completeTransfer(@NotNull UUID entityId, boolean success) {
 PendingTransferInfo info = pendingTransfers.remove(entityId);
 if (info != null) {
 pendingTransfers.remove(info.transferId());
 info.completionFuture().complete(success);
 logger.debug("Completed transfer {} for entity {} with success={}",
 info.transferId(), entityId, success);
 }
 }

 /**
 * Gets a pending transfer by entity ID.
 *
 * @param entityId the entity ID
 * @return the pending transfer info, or null if not found
 */
 @Nullable
 public PendingTransferInfo getPendingTransfer(@NotNull UUID entityId) {
 return pendingTransfers.get(entityId);
 }

 /**
 * Waits for a transfer confirmation for the given entity.
 *
 * @param entityId the entity ID to wait for
 * @return a CompletableFuture that will complete with the confirmation
 */
 @NotNull
 public CompletableFuture<TransferConfirmation> waitForConfirmation(@NotNull UUID entityId) {
 return confirmationFutures.computeIfAbsent(entityId,
 id -> new CompletableFuture<TransferConfirmation>()
 .orTimeout(PENDING_TRANSFER_TIMEOUT_MS / 1000, TimeUnit.SECONDS));
 }

 /**
 * Checks if there is a pending transfer for the given entity.
 *
 * @param entityId the entity ID
 * @return true if a transfer is pending
 */
 public boolean hasPendingTransfer(@NotNull UUID entityId) {
 return pendingTransfers.containsKey(entityId);
 }

 /**
 * Gets the count of currently pending transfers.
 *
 * @return the number of pending transfers
 */
 public int getPendingTransferCount() {
 // Divide by 2 because we store each transfer twice (by ID and by entity ID)
 return pendingTransfers.size() / 2;
 }

 /**
 * Cleans up expired pending transfers.
 */
 private void cleanupExpiredTransfers() {
 long now = System.currentTimeMillis();
 int removed = 0;

 for (Map.Entry<UUID, PendingTransferInfo> entry : pendingTransfers.entrySet()) {
 PendingTransferInfo info = entry.getValue();
 if (now - info.timestamp() > PENDING_TRANSFER_TIMEOUT_MS) {
 // Complete with failure
 info.completionFuture().complete(false);
 pendingTransfers.remove(info.transferId());
 pendingTransfers.remove(info.entityId());
 removed++;
 }
 }

 if (removed > 0) {
 logger.debug("Cleaned up {} expired pending transfers", removed);
 }
 }

 /**
 * Starts the periodic cleanup task.
 */
 private void startCleanupTask() {
 // Note: In a real implementation, this would use the proxy scheduler
 // For now, we rely on timeout handling in the futures
 }

 /**
 * Shuts down the network bridge and cleans up resources.
 */
 public void shutdown() {
 // Complete all pending futures with failure
 for (PendingTransferInfo info : pendingTransfers.values()) {
 if (!info.completionFuture().isDone()) {
 info.completionFuture().complete(false);
 }
 }
 pendingTransfers.clear();

 for (CompletableFuture<TransferConfirmation> future : confirmationFutures.values()) {
 if (!future.isDone()) {
 future.completeExceptionally(new IllegalStateException("Network bridge shutting down"));
 }
 }
 confirmationFutures.clear();

 logger.info("VelocityNetworkBridge shut down");
 }

 /**
 * Internal message wrapper for proxy communication.
 */
 private record ProxyMessage(
 String channel,
 String payload,
 long timestamp
 ) {}
}
