package de.t14d3.rapunzelcore.modules.portals.network;

import de.t14d3.rapunzelcore.modules.portals.PortalAction;
import de.t14d3.rapunzelcore.modules.portals.ParticleConfig;
import de.t14d3.rapunzelcore.modules.portals.PortalType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Network payload for portal data transfer between servers.
 * Used for cross-server portal synchronization and entity transfers.
 * Simplified to include PortalAction list.
 */
public record PortalPayload(
 @NotNull UUID id,
 @NotNull String name,
 @NotNull String world,
 double minX, double minY, double minZ,
 double maxX, double maxY, double maxZ,
 @Nullable String targetServer,
 @Nullable String targetWorld,
 double targetX, double targetY, double targetZ,
 float targetYaw, float targetPitch,
 @NotNull PortalType type,
 @NotNull List<PortalAction> actions,
 @Nullable ParticlePayload particlePayload,
 boolean enabled,
 boolean allowEntities,
 boolean allowPlayers,
 @Nullable String permission,
 @NotNull Map<String, String> metadata,
 @NotNull Instant createdAt,
 @Nullable Instant updatedAt
) {
 /**
 * Payload for particle configuration serialization.
 */
 public record ParticlePayload(
 @NotNull String particleType,
 int count,
 double offsetX, double offsetY, double offsetZ,
 double speed,
 boolean enabled,
 @Nullable String color,
 int intervalTicks
 ) {
 public static ParticlePayload fromConfig(@Nullable ParticleConfig config) {
 if (config == null) return null;
 return new ParticlePayload(
 config.particleType(),
 config.count(),
 config.offsetX(), config.offsetY(), config.offsetZ(),
 config.speed(),
 config.enabled(),
 config.color(),
 config.intervalTicks()
 );
 }

 public ParticleConfig toConfig() {
 return new ParticleConfig(
 particleType, count, offsetX, offsetY, offsetZ,
 speed, enabled, color, intervalTicks
 );
 }
 }
}
