package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Entity representing a player ticket/report in the database.
 * Used to track and manage player support tickets across servers.
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id(autoIncrement = true)
    @Column(name = "id")
    private long id;

    @Column(name = "ticket_number", nullable = false, type = "VARCHAR(16)")
    private String ticketNumber;

    @Column(name = "player_uuid", nullable = false, type = "VARCHAR(36)")
    private String playerUuid;

    @Column(name = "player_name", nullable = false, type = "VARCHAR(32)")
    private String playerName;

    @Column(name = "category", nullable = false, type = "VARCHAR(32)")
    private String category;

    @Column(name = "priority", nullable = false, type = "VARCHAR(16)")
    private String priority;

    @Column(name = "subject", nullable = false, type = "VARCHAR(128)")
    private String subject;

    @Column(name = "description", nullable = false, type = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, type = "VARCHAR(16)")
    private String status;

    @Column(name = "assigned_to", nullable = true, type = "VARCHAR(36)")
    private String assignedTo;

    @Column(name = "server", nullable = false, type = "VARCHAR(64)")
    private String server;

    @Column(name = "location_world", nullable = false, type = "VARCHAR(64)")
    private String locationWorld;

    @Column(name = "location_x", nullable = false, type = "DOUBLE")
    private double locationX;

    @Column(name = "location_y", nullable = false, type = "DOUBLE")
    private double locationY;

    @Column(name = "location_z", nullable = false, type = "DOUBLE")
    private double locationZ;

    @Column(name = "created_at", nullable = false, type = "BIGINT")
    private long createdAt;

    @Column(name = "updated_at", nullable = false, type = "BIGINT")
    private long updatedAt;

    @Column(name = "resolved_at", nullable = true, type = "BIGINT")
    private Long resolvedAt;

    @Column(name = "resolved_by", nullable = true, type = "VARCHAR(36)")
    private String resolvedBy;

    @Column(name = "resolution", nullable = true, type = "TEXT")
    private String resolution;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getLocationWorld() {
        return locationWorld;
    }

    public void setLocationWorld(String locationWorld) {
        this.locationWorld = locationWorld;
    }

    public double getLocationX() {
        return locationX;
    }

    public void setLocationX(double locationX) {
        this.locationX = locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public void setLocationY(double locationY) {
        this.locationY = locationY;
    }

    public double getLocationZ() {
        return locationZ;
    }

    public void setLocationZ(double locationZ) {
        this.locationZ = locationZ;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Long resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", ticketNumber='" + ticketNumber + '\'' +
                ", playerUuid='" + playerUuid + '\'' +
                ", playerName='" + playerName + '\'' +
                ", category='" + category + '\'' +
                ", priority='" + priority + '\'' +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", assignedTo='" + assignedTo + '\'' +
                ", server='" + server + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
