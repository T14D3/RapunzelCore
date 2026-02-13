package de.t14d3.rapunzelcore.modules.portals.network;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Sealed interface for portal network message types.
 * Defines all possible portal synchronization messages between servers.
 */
public sealed interface PortalNetworkMessage {

 /**
  * Channel name for portal network messages.
  */
  String CHANNEL = "rapunzelcore:portal_sync";

 /**
  * Message sent when a portal is created or updated.
  */
  record PortalSync(@NotNull PortalPayload portal) implements PortalNetworkMessage {}

 /**
  * Message sent when a portal is deleted.
  */
  record PortalDelete(@NotNull UUID portalId, @NotNull String sourceServer) implements PortalNetworkMessage {}

 /**
  * Message sent when an entity enters a portal targeting another server.
  */
  record EntityPortalEntry(
  @NotNull UUID entityUuid,
  @NotNull String entityType,
  @NotNull UUID portalId,
  @NotNull String sourceServer,
  @NotNull String targetServer,
  byte[] serializedEntityData
 ) implements PortalNetworkMessage {}

 /**
  * Message sent to confirm entity transfer completion.
  */
  record TransferConfirm(
  @NotNull UUID originalEntityUuid,
  @NotNull UUID newEntityUuid,
  boolean success,
  @NotNull String sourceServer
 ) implements PortalNetworkMessage {}

 /**
  * Message sent to request portal data from another server.
  */
  record PortalDataRequest(@NotNull String requestingServer) implements PortalNetworkMessage {}

 /**
  * Message sent in response to portal data request.
  */
  record PortalDataResponse(
  @NotNull java.util.List<PortalPayload> portals,
  @NotNull String sourceServer
 ) implements PortalNetworkMessage {}
}
