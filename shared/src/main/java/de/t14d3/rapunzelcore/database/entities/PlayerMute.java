package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Entity representing a player mute in the database.
 * Used to track and enforce mutes across servers.
 */
@Entity
@Table(name = "player_mutes")
public class PlayerMute {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "player_uuid", nullable = false, type = "VARCHAR(36)")
    private String playerUuid;

    @Column(name = "muted_by", nullable = false, type = "VARCHAR(36)")
    private String mutedBy;

    @Column(name = "reason", nullable = false, type = "VARCHAR(255)")
    private String reason;

    @Column(name = "expires_at", nullable = false, type = "BIGINT")
    private long expiresAt;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    @Column(name = "server", nullable = false, type = "VARCHAR(64)")
    private String server;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getMutedBy() {
        return mutedBy;
    }

    public void setMutedBy(String mutedBy) {
        this.mutedBy = mutedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    /**
     * Checks if this mute has expired.
     * @return true if the current time is past the expiration time, or false if permanent (-1)
     */
    public boolean isExpired() {
        if (expiresAt == -1) {
            return false; // Permanent mute
        }
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Checks if this is a permanent mute.
     * @return true if the mute is permanent (expiresAt == -1)
     */
    public boolean isPermanent() {
        return expiresAt == -1;
    }

    @Override
    public String toString() {
        return "PlayerMute{" +
                "id=" + id +
                ", playerUuid='" + playerUuid + '\'' +
                ", mutedBy='" + mutedBy + '\'' +
                ", reason='" + reason + '\'' +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", server='" + server + '\'' +
                '}';
    }
}
