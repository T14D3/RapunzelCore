package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Entity representing a player warning in the database.
 * Used to track and manage player warnings across servers.
 */
@Entity
@Table(name = "player_warnings")
public class PlayerWarning {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "player_uuid", nullable = false, type = "VARCHAR(36)")
    private String playerUuid;

    @Column(name = "warned_by", nullable = false, type = "VARCHAR(36)")
    private String warnedBy;

    @Column(name = "reason", nullable = false, type = "VARCHAR(255)")
    private String reason;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    @Column(name = "server", nullable = false, type = "VARCHAR(64)")
    private String server;

    @Column(name = "active", nullable = false, type = "BOOLEAN")
    private boolean active;

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

    public String getWarnedBy() {
        return warnedBy;
    }

    public void setWarnedBy(String warnedBy) {
        this.warnedBy = warnedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "PlayerWarning{" +
                "id=" + id +
                ", playerUuid='" + playerUuid + '\'' +
                ", warnedBy='" + warnedBy + '\'' +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                ", server='" + server + '\'' +
                ", active=" + active +
                '}';
    }
}
