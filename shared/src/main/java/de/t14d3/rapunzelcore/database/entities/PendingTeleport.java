package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

@Entity
@Table(name = "pending_teleports")
public class PendingTeleport {
    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "player_uuid", nullable = false, type = "VARCHAR(36)")
    private String playerUuid;

    @Column(name = "target_server", nullable = false)
    private String targetServer;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "arg", nullable = true)
    private String arg;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    public PendingTeleport() {
    }

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

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

