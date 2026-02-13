package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;
import de.t14d3.rapunzelcore.modules.portals.ParticleConfig;
import de.t14d3.rapunzelcore.modules.portals.Portal;
import de.t14d3.rapunzelcore.modules.portals.PortalAction;
import de.t14d3.rapunzelcore.modules.portals.PortalType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity representing a portal in the database.
 *
 *
 * This entity maps the {@link Portal} domain object to database columns using Spool ORM annotations.
 * It handles serialization of complex types (actions, metadata) to JSON for database storage.
 *
 *
 *
 * The entity supports:
 *
 * - Portal bounds and location data
 * - Target server/world for cross-server portals
 * - Particle effect configuration
 * - Portal actions (teleport, commands, etc.)
 * - Permission-based access control
 * - Metadata for extensibility
 *
 * @see Portal
 * @see de.t14d3.rapunzelcore.modules.portals.database.PortalRepository
 */
@Entity
@Table(name = "rapunzel_portals")
public class PortalEntity {

 @Id(autoIncrement = false)
 @Column(name = "id")
 private UUID id;

 @Column(name = "name")
 private String name;

 @Column(name = "world")
 private String world;

 @Column(name = "min_x")
 private double minX;

 @Column(name = "min_y")
 private double minY;

 @Column(name = "min_z")
 private double minZ;

 @Column(name = "max_x")
 private double maxX;

 @Column(name = "max_y")
 private double maxY;

 @Column(name = "max_z")
 private double maxZ;

 @Column(name = "target_server")
 private String targetServer;

 @Column(name = "target_world")
 private String targetWorld;

 @Column(name = "target_x")
 private double targetX;

 @Column(name = "target_y")
 private double targetY;

 @Column(name = "target_z")
 private double targetZ;

 @Column(name = "target_yaw")
 private float targetYaw;

 @Column(name = "target_pitch")
 private float targetPitch;

 @Column(name = "type")
 private String type;

 @Column(name = "actions", type = "TEXT")
 private String actionsJson;

 @Column(name = "particle_type")
 private String particleType;

 @Column(name = "particle_count")
 private Integer particleCount;

 @Column(name = "particle_offset_x")
 private Double particleOffsetX;

 @Column(name = "particle_offset_y")
 private Double particleOffsetY;

 @Column(name = "particle_offset_z")
 private Double particleOffsetZ;

 @Column(name = "particle_speed")
 private Double particleSpeed;

 @Column(name = "particle_enabled")
 private Boolean particleEnabled;

 @Column(name = "particle_color")
 private String particleColor;

 @Column(name = "particle_interval")
 private Integer particleInterval;

 @Column(name = "enabled")
 private boolean enabled;

 @Column(name = "allow_entities")
 private boolean allowEntities;

 @Column(name = "allow_players")
 private boolean allowPlayers;

 @Column(name = "permission")
 private String permission;

 @Column(name = "metadata", type = "TEXT")
 private String metadataJson;

 @Column(name = "created_at")
 private String createdAt;

 @Column(name = "updated_at")
 private String updatedAt;

 private static final Gson GSON = new Gson();
 private static final Type ACTION_LIST_TYPE = new TypeToken<List<PortalAction>>(){}.getType();
 private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();

/**
 * Default constructor required by JPA.
 */
 public PortalEntity() {}

/**
 * Creates a PortalEntity from a Portal domain object.
 *
 * @param portal the portal to convert
 * @throws NullPointerException if portal is null
 */
 public PortalEntity(@NotNull Portal portal) {
 this.id = portal.id();
 this.name = portal.name();
 this.world = portal.world();
 this.minX = portal.minX();
 this.minY = portal.minY();
 this.minZ = portal.minZ();
 this.maxX = portal.maxX();
 this.maxY = portal.maxY();
 this.maxZ = portal.maxZ();
 this.targetServer = portal.targetServer();
 this.targetWorld = portal.targetWorld();
 this.targetX = portal.targetX();
 this.targetY = portal.targetY();
 this.targetZ = portal.targetZ();
 this.targetYaw = portal.targetYaw();
 this.targetPitch = portal.targetPitch();
 this.type = portal.type().name();
 this.actionsJson = GSON.toJson(portal.actions());
 
 if (portal.particleConfig() != null) {
 this.particleType = portal.particleConfig().particleType();
 this.particleCount = portal.particleConfig().count();
 this.particleOffsetX = portal.particleConfig().offsetX();
 this.particleOffsetY = portal.particleConfig().offsetY();
 this.particleOffsetZ = portal.particleConfig().offsetZ();
 this.particleSpeed = portal.particleConfig().speed();
 this.particleEnabled = portal.particleConfig().enabled();
 this.particleColor = portal.particleConfig().color();
 this.particleInterval = portal.particleConfig().intervalTicks();
 }
 
 this.enabled = portal.enabled();
 this.allowEntities = portal.allowEntities();
 this.allowPlayers = portal.allowPlayers();
 this.permission = portal.permission();
 this.metadataJson = GSON.toJson(portal.metadata());
 this.createdAt = portal.createdAt().toString();
 this.updatedAt = portal.updatedAt() != null ? portal.updatedAt().toString() : null;
 }

/**
 * Converts this entity to a Portal domain object.
 *
 * @return the portal domain object
 */
 @NotNull
 public Portal toPortal() {
 ParticleConfig particleConfig = null;
 if (particleType != null) {
 particleConfig = new ParticleConfig(
 particleType,
 particleCount != null ? particleCount : 5,
 particleOffsetX != null ? particleOffsetX : 0.5,
 particleOffsetY != null ? particleOffsetY : 0.5,
 particleOffsetZ != null ? particleOffsetZ : 0.5,
 particleSpeed != null ? particleSpeed : 0.1,
 particleEnabled != null ? particleEnabled : true,
 particleColor,
 particleInterval != null ? particleInterval : 5
 );
 }

 List<PortalAction> actions = List.of();
 if (actionsJson != null && !actionsJson.isEmpty()) {
 try {
 actions = GSON.fromJson(actionsJson, ACTION_LIST_TYPE);
 } catch (Exception e) {
 actions = List.of();
 }
 }

 Map<String, Object> metadata = Map.of();
 if (metadataJson != null && !metadataJson.isEmpty()) {
 try {
 metadata = GSON.fromJson(metadataJson, MAP_TYPE);
 } catch (Exception e) {
 metadata = Map.of();
 }
 }

 PortalType portalType = PortalType.valueOf(type != null ? type : "TELEPORT");

 return Portal.builder()
 .id(id)
 .name(name)
 .world(world)
 .bounds(minX, minY, minZ, maxX, maxY, maxZ)
 .targetServer(targetServer)
 .targetWorld(targetWorld)
 .targetLocation(targetX, targetY, targetZ)
 .targetRotation(targetYaw, targetPitch)
 .type(portalType)
 .actions(actions)
 .particleConfig(particleConfig)
 .enabled(enabled)
 .allowEntities(allowEntities)
 .allowPlayers(allowPlayers)
 .permission(permission)
 .metadata(metadata)
 .createdAt(createdAt != null ? Instant.parse(createdAt) : Instant.now())
 .updatedAt(updatedAt != null ? Instant.parse(updatedAt) : null)
 .build();
 }

 // Getters and setters
 public UUID getId() { return id; }
 public void setId(UUID id) { this.id = id; }

 public String getName() { return name; }
 public void setName(String name) { this.name = name; }

 public String getWorld() { return world; }
 public void setWorld(String world) { this.world = world; }

 public double getMinX() { return minX; }
 public void setMinX(double minX) { this.minX = minX; }

 public double getMinY() { return minY; }
 public void setMinY(double minY) { this.minY = minY; }

 public double getMinZ() { return minZ; }
 public void setMinZ(double minZ) { this.minZ = minZ; }

 public double getMaxX() { return maxX; }
 public void setMaxX(double maxX) { this.maxX = maxX; }

 public double getMaxY() { return maxY; }
 public void setMaxY(double maxY) { this.maxY = maxY; }

 public double getMaxZ() { return maxZ; }
 public void setMaxZ(double maxZ) { this.maxZ = maxZ; }

 public String getTargetServer() { return targetServer; }
 public void setTargetServer(String targetServer) { this.targetServer = targetServer; }

 public String getTargetWorld() { return targetWorld; }
 public void setTargetWorld(String targetWorld) { this.targetWorld = targetWorld; }

 public double getTargetX() { return targetX; }
 public void setTargetX(double targetX) { this.targetX = targetX; }

 public double getTargetY() { return targetY; }
 public void setTargetY(double targetY) { this.targetY = targetY; }

 public double getTargetZ() { return targetZ; }
 public void setTargetZ(double targetZ) { this.targetZ = targetZ; }

 public float getTargetYaw() { return targetYaw; }
 public void setTargetYaw(float targetYaw) { this.targetYaw = targetYaw; }

 public float getTargetPitch() { return targetPitch; }
 public void setTargetPitch(float targetPitch) { this.targetPitch = targetPitch; }

 public String getType() { return type; }
 public void setType(String type) { this.type = type; }

 public String getActionsJson() { return actionsJson; }
 public void setActionsJson(String actionsJson) { this.actionsJson = actionsJson; }

 public String getParticleType() { return particleType; }
 public void setParticleType(String particleType) { this.particleType = particleType; }

 public Integer getParticleCount() { return particleCount; }
 public void setParticleCount(Integer particleCount) { this.particleCount = particleCount; }

 public Double getParticleOffsetX() { return particleOffsetX; }
 public void setParticleOffsetX(Double particleOffsetX) { this.particleOffsetX = particleOffsetX; }

 public Double getParticleOffsetY() { return particleOffsetY; }
 public void setParticleOffsetY(Double particleOffsetY) { this.particleOffsetY = particleOffsetY; }

 public Double getParticleOffsetZ() { return particleOffsetZ; }
 public void setParticleOffsetZ(Double particleOffsetZ) { this.particleOffsetZ = particleOffsetZ; }

 public Double getParticleSpeed() { return particleSpeed; }
 public void setParticleSpeed(Double particleSpeed) { this.particleSpeed = particleSpeed; }

 public Boolean getParticleEnabled() { return particleEnabled; }
 public void setParticleEnabled(Boolean particleEnabled) { this.particleEnabled = particleEnabled; }

 public String getParticleColor() { return particleColor; }
 public void setParticleColor(String particleColor) { this.particleColor = particleColor; }

 public Integer getParticleInterval() { return particleInterval; }
 public void setParticleInterval(Integer particleInterval) { this.particleInterval = particleInterval; }

 public boolean isEnabled() { return enabled; }
 public void setEnabled(boolean enabled) { this.enabled = enabled; }

 public boolean isAllowEntities() { return allowEntities; }
 public void setAllowEntities(boolean allowEntities) { this.allowEntities = allowEntities; }

 public boolean isAllowPlayers() { return allowPlayers; }
 public void setAllowPlayers(boolean allowPlayers) { this.allowPlayers = allowPlayers; }

 public String getPermission() { return permission; }
 public void setPermission(String permission) { this.permission = permission; }

 public String getMetadataJson() { return metadataJson; }
 public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

 public String getCreatedAt() { return createdAt; }
 public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

 public String getUpdatedAt() { return updatedAt; }
 public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
