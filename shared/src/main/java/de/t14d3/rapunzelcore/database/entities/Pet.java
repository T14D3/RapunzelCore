
package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.*;

/**
 * Entity representing a player's pet in the database.
 * Tracks pet ownership, protection status, and cross-server location.
 */
@Entity
@Table(name = "pets")
public class Pet {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private Long id;

    @Column(name = "owner_uuid", nullable = false, type = "VARCHAR(36)")
    private String ownerUuid;

    @Column(name = "entity_uuid", nullable = false, type = "VARCHAR(36)")
    private String entityUuid;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "custom_name", nullable = true)
    private String customName;

    @Column(name = "allowed_players", nullable = true, type = "TEXT")
    private String allowedPlayers;

    @Column(name = "protected", nullable = false, type = "BOOLEAN")
    private boolean protectedPet = true;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private long lastSeenAt;

    @Column(name = "last_server", nullable = true)
    private String lastServer;

    public Pet() {
        this.createdAt = System.currentTimeMillis();
        this.lastSeenAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getEntityUuid() {
        return entityUuid;
    }

    public void setEntityUuid(String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    /**
     * Gets the allowed players as a JSON array string.
     * @return JSON array of player UUIDs allowed to interact with this pet
     */
    public String getAllowedPlayers() {
        return allowedPlayers;
    }

    /**
     * Sets the allowed players as a JSON array string.
     * @param allowedPlayers JSON array of player UUIDs
     */
    public void setAllowedPlayers(String allowedPlayers) {
        this.allowedPlayers = allowedPlayers;
    }

    public boolean isProtected() {
        return protectedPet;
    }

    public void setProtected(boolean protectedPet) {
        this.protectedPet = protectedPet;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(long lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getLastServer() {
        return lastServer;
    }

    public void setLastServer(String lastServer) {
        this.lastServer = lastServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pet pet)) return false;
        return id != null && id.equals(pet.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
