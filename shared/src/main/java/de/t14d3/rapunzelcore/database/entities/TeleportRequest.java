package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

@Entity
@Table(name = "teleport_requests")
public class TeleportRequest {
    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "requester_uuid", nullable = false, type = "VARCHAR(36)")
    private String requesterUuid;

    @Column(name = "requester_name", nullable = false)
    private String requesterName;

    @Column(name = "target_uuid", nullable = false, type = "VARCHAR(36)")
    private String targetUuid;

    @Column(name = "requester_server", nullable = false)
    private String requesterServer;

    @Column(name = "target_server", nullable = false)
    private String targetServer;

    @Column(name = "is_tpa_here", nullable = false, type = "BOOLEAN")
    private boolean tpaHere;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    public TeleportRequest() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRequesterUuid() {
        return requesterUuid;
    }

    public void setRequesterUuid(String requesterUuid) {
        this.requesterUuid = requesterUuid;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(String targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getRequesterServer() {
        return requesterServer;
    }

    public void setRequesterServer(String requesterServer) {
        this.requesterServer = requesterServer;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public boolean isTpaHere() {
        return tpaHere;
    }

    public void setTpaHere(boolean tpaHere) {
        this.tpaHere = tpaHere;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}

