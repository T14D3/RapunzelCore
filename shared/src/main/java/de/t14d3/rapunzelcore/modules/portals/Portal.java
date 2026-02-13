package de.t14d3.rapunzelcore.modules.portals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a portal with customizable area, actions, and optional particle visuals.
 * Portals can trigger actions when players or entities enter them.
 * Simplified to use PortalAction list instead of separate target fields.
 */
public record Portal(
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
    @Nullable ParticleConfig particleConfig,
    boolean enabled,
    boolean allowEntities,
    boolean allowPlayers,
    @Nullable String permission,
    @NotNull Map<String, Object> metadata,
    @NotNull Instant createdAt,
    @Nullable Instant updatedAt
) {
    public Portal {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(actions, "actions cannot be null");
        Objects.requireNonNull(metadata, "metadata cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        actions = List.copyOf(actions);
        metadata = Map.copyOf(metadata);
    }

    /**
     * Checks if a location is within this portal's bounds.
     *
     * @param world the world name
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return true if the location is within the portal
     */
    public boolean contains(@NotNull String world, double x, double y, double z) {
        return this.world.equals(world) &&
               x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * Checks if this portal transfers to another server.
     *
     * @return true if this is a cross-server portal
     */
    public boolean isCrossServer() {
        return targetServer != null && !targetServer.isEmpty();
    }

    /**
     * Gets the center point of this portal.
     *
     * @return array containing [x, y, z]
     */
    public double[] getCenter() {
        return new double[] {
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        };
    }

    /**
     * Gets the volume of this portal in cubic blocks.
     *
     * @return the volume
     */
    public double getVolume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    /**
     * Converts legacy target fields into a TeleportAction.
     * Useful for migration from old portal data.
     */
    public PortalAction asTeleportAction() {
        if (targetWorld == null) {
            throw new IllegalStateException("Cannot convert portal without target world");
        }
        return new PortalAction.TeleportAction(
            targetWorld,
            targetX, targetY, targetZ,
            targetYaw, targetPitch,
            targetServer,
            0, 0, Map.of()
        );
    }

    /**
     * Builder for creating Portal instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String name;
        private String world;
        private double minX, minY, minZ;
        private double maxX, maxY, maxZ;
        private String targetServer;
        private String targetWorld;
        private double targetX, targetY, targetZ;
        private float targetYaw, targetPitch;
        private PortalType type = PortalType.TELEPORT;
        private List<PortalAction> actions = List.of();
        private ParticleConfig particleConfig;
        private boolean enabled = true;
        private boolean allowEntities = true;
        private boolean allowPlayers = true;
        private String permission;
        private Map<String, Object> metadata = Map.of();
        private Instant createdAt = Instant.now();
        private Instant updatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder world(String world) {
            this.world = world;
            return this;
        }

        public Builder bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = Math.min(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxY = Math.max(minY, maxY);
            this.maxZ = Math.max(minZ, maxZ);
            return this;
        }

        public Builder targetServer(String targetServer) {
            this.targetServer = targetServer;
            return this;
        }

        public Builder targetWorld(String targetWorld) {
            this.targetWorld = targetWorld;
            return this;
        }

        public Builder targetLocation(double x, double y, double z) {
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
            return this;
        }

        public Builder targetRotation(float yaw, float pitch) {
            this.targetYaw = yaw;
            this.targetPitch = pitch;
            return this;
        }

        public Builder type(PortalType type) {
            this.type = type;
            return this;
        }

        public Builder actions(List<PortalAction> actions) {
            this.actions = actions != null ? List.copyOf(actions) : List.of();
            return this;
        }

        public Builder addAction(PortalAction action) {
            var list = new java.util.ArrayList<>(this.actions);
            list.add(action);
            this.actions = List.copyOf(list);
            return this;
        }

        public Builder particleConfig(ParticleConfig particleConfig) {
            this.particleConfig = particleConfig;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder allowEntities(boolean allowEntities) {
            this.allowEntities = allowEntities;
            return this;
        }

        public Builder allowPlayers(boolean allowPlayers) {
            this.allowPlayers = allowPlayers;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Portal build() {
            return new Portal(
                id, name, world, minX, minY, minZ, maxX, maxY, maxZ,
                targetServer, targetWorld, targetX, targetY, targetZ, targetYaw, targetPitch,
                type, actions, particleConfig, enabled, allowEntities, allowPlayers,
                permission, metadata, createdAt, updatedAt
            );
        }
    }
}
