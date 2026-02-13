package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Unified inventory profile entity.
 * Stores both persistent inventory state and snapshot data.
 * 
 * When snapshotType is "CURRENT", this represents the player's current inventory
 * for a specific world group and game mode context.
 * When snapshotType is "MANUAL" or "AUTO", this represents a named snapshot
 * that can be restored later.
 */
@Entity
@Table(name = "inventory_profiles")
public class InventoryProfile {

 @Id(autoIncrement = true)
 @Column(name = "id")
 private long id;

 // Player identification
 @Column(name = "player_uuid", nullable = false, type = "VARCHAR(36)")
 private String playerUuid;

 @Column(name = "player_name", type = "VARCHAR(32)")
 private String playerName;

 // Context/Key fields (for current inventory state)
 @Column(name = "context_key", type = "VARCHAR(128)")
 private String contextKey;

 // Snapshot metadata (for named snapshots)
 @Column(name = "snapshot_name", type = "VARCHAR(64)")
 private String snapshotName;

 @Column(name = "snapshot_type", nullable = false, type = "VARCHAR(16)")
 private String snapshotType = "CURRENT";

 @Column(name = "created_by", type = "VARCHAR(36)")
 private String createdBy;

 @Column(name = "reason", type = "VARCHAR(255)")
 private String reason;

 // World/GameMode context
 @Column(name = "world_group", nullable = false, type = "VARCHAR(64)")
 private String worldGroup;

 @Column(name = "game_mode", nullable = false, type = "VARCHAR(32)")
 private String gameMode;

 // Location (for snapshots)
 @Column(name = "location_world", type = "VARCHAR(64)")
 private String locationWorld;

 @Column(name = "location_x", type = "DOUBLE")
 private double locationX;

 @Column(name = "location_y", type = "DOUBLE")
 private double locationY;

 @Column(name = "location_z", type = "DOUBLE")
 private double locationZ;

 @Column(name = "yaw", type = "FLOAT")
 private float yaw;

 @Column(name = "pitch", type = "FLOAT")
 private float pitch;

 // Inventory data (serialized)
 @Column(name = "inventory_data", type = "BLOB")
 private byte[] inventoryData;

 @Column(name = "armor_data", type = "BLOB")
 private byte[] armorData;

 @Column(name = "extra_data", type = "BLOB")
 private byte[] extraData;

 @Column(name = "ender_chest_data", type = "BLOB")
 private byte[] enderChestData;

 @Column(name = "potion_data", type = "BLOB")
 private byte[] potionData;

 // Player state
 @Column(name = "health", type = "DOUBLE")
 private double health;

 @Column(name = "food_level", type = "INT")
 private int foodLevel;

 @Column(name = "saturation", type = "FLOAT")
 private float saturation;

 @Column(name = "exp_level", type = "INT")
 private int expLevel;

 @Column(name = "exp_progress", type = "FLOAT")
 private float expProgress;

 @Column(name = "held_slot", type = "INT")
 private int heldSlot;

 // Timestamps
 @Column(name = "created_at", type = "BIGINT")
 private long createdAt;

 @Column(name = "updated_at", type = "BIGINT")
 private long updatedAt;

 // Getters and Setters

 public long getId() { return id; }
 public void setId(long id) { this.id = id; }

 public String getPlayerUuid() { return playerUuid; }
 public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

 public String getPlayerName() { return playerName; }
 public void setPlayerName(String playerName) { this.playerName = playerName; }

 public String getContextKey() { return contextKey; }
 public void setContextKey(String contextKey) { this.contextKey = contextKey; }

 public String getSnapshotName() { return snapshotName; }
 public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }

 public String getSnapshotType() { return snapshotType; }
 public void setSnapshotType(String snapshotType) { this.snapshotType = snapshotType; }

 public String getCreatedBy() { return createdBy; }
 public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

 public String getReason() { return reason; }
 public void setReason(String reason) { this.reason = reason; }

 public String getWorldGroup() { return worldGroup; }
 public void setWorldGroup(String worldGroup) { this.worldGroup = worldGroup; }

 public String getGameMode() { return gameMode; }
 public void setGameMode(String gameMode) { this.gameMode = gameMode; }

 public String getLocationWorld() { return locationWorld; }
 public void setLocationWorld(String locationWorld) { this.locationWorld = locationWorld; }

 public double getLocationX() { return locationX; }
 public void setLocationX(double locationX) { this.locationX = locationX; }

 public double getLocationY() { return locationY; }
 public void setLocationY(double locationY) { this.locationY = locationY; }

 public double getLocationZ() { return locationZ; }
 public void setLocationZ(double locationZ) { this.locationZ = locationZ; }

 public float getYaw() { return yaw; }
 public void setYaw(float yaw) { this.yaw = yaw; }

 public float getPitch() { return pitch; }
 public void setPitch(float pitch) { this.pitch = pitch; }

 public byte[] getInventoryData() { return inventoryData; }
 public void setInventoryData(byte[] inventoryData) { this.inventoryData = inventoryData; }

 public byte[] getArmorData() { return armorData; }
 public void setArmorData(byte[] armorData) { this.armorData = armorData; }

 public byte[] getExtraData() { return extraData; }
 public void setExtraData(byte[] extraData) { this.extraData = extraData; }

 public byte[] getEnderChestData() { return enderChestData; }
 public void setEnderChestData(byte[] enderChestData) { this.enderChestData = enderChestData; }

 public byte[] getPotionData() { return potionData; }
 public void setPotionData(byte[] potionData) { this.potionData = potionData; }

 public double getHealth() { return health; }
 public void setHealth(double health) { this.health = health; }

 public int getFoodLevel() { return foodLevel; }
 public void setFoodLevel(int foodLevel) { this.foodLevel = foodLevel; }

 public float getSaturation() { return saturation; }
 public void setSaturation(float saturation) { this.saturation = saturation; }

 public int getExpLevel() { return expLevel; }
 public void setExpLevel(int expLevel) { this.expLevel = expLevel; }

 public float getExpProgress() { return expProgress; }
 public void setExpProgress(float expProgress) { this.expProgress = expProgress; }

 public int getHeldSlot() { return heldSlot; }
 public void setHeldSlot(int heldSlot) { this.heldSlot = heldSlot; }

 public long getCreatedAt() { return createdAt; }
 public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

 public long getUpdatedAt() { return updatedAt; }
 public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

 /**
 * Returns true if this profile represents a named snapshot (not current state).
 */
 public boolean isSnapshot() {
 return !"CURRENT".equals(snapshotType);
 }

 /**
 * Returns the effective name for display purposes.
 */
 public String getDisplayName() {
 if (snapshotName != null && !snapshotName.isEmpty()) {
 return snapshotName;
 }
 if (isSnapshot()) {
 return snapshotType + "-" + id;
 }
 return contextKey != null ? contextKey : "unknown";
 }
}
