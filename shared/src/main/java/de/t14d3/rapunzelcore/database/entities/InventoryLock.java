package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Distributed lock entity for cross-server inventory synchronization.
 * Used by DatabaseDistributedLock to coordinate inventory access across multiple servers.
 */
@Entity
@Table(name = "inventory_locks")
public class InventoryLock {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "lock_key", nullable = false, type = "VARCHAR(255)")
    private String lockKey;

    @Column(name = "node_id", nullable = false, type = "VARCHAR(255)")
    private String nodeId;

    @Column(name = "acquired_at", nullable = false, type = "BIGINT")
    private long acquiredAt;

    @Column(name = "expires_at", nullable = false, type = "BIGINT")
    private long expiresAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(long acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Checks if this lock has expired.
     * @return true if the current time is past the expiration time
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    @Override
    public String toString() {
        return "InventoryLock{" +
                "id=" + id +
                ", lockKey='" + lockKey + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", acquiredAt=" + acquiredAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
