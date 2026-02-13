package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.Column;
import de.t14d3.spool.annotations.Entity;
import de.t14d3.spool.annotations.Id;
import de.t14d3.spool.annotations.Table;

/**
 * Entity representing a comment on a ticket in the database.
 * Used to track staff and player comments on support tickets.
 */
@Entity
@Table(name = "ticket_comments")
public class TicketComment {

 @Id(autoIncrement = true)
 @Column(name = "id")
 private long id;

 @Column(name = "ticket_id", nullable = false, type = "BIGINT")
 private long ticketId;

 @Column(name = "author_uuid", nullable = false, type = "VARCHAR(36)")
 private String authorUuid;

 @Column(name = "author_name", nullable = false, type = "VARCHAR(32)")
 private String authorName;

 @Column(name = "is_staff", nullable = false, type = "BOOLEAN")
 private boolean isStaff;

 @Column(name = "comment", nullable = false, type = "TEXT")
 private String comment;

 @Column(name = "created_at", nullable = false, type = "BIGINT")
 private long createdAt;

 public long getId() {
 return id;
 }

 public void setId(long id) {
 this.id = id;
 }

 public long getTicketId() {
 return ticketId;
 }

 public void setTicketId(long ticketId) {
 this.ticketId = ticketId;
 }

 public String getAuthorUuid() {
 return authorUuid;
 }

 public void setAuthorUuid(String authorUuid) {
 this.authorUuid = authorUuid;
 }

 public String getAuthorName() {
 return authorName;
 }

 public void setAuthorName(String authorName) {
 this.authorName = authorName;
 }

 public boolean isStaff() {
 return isStaff;
 }

 public void setStaff(boolean staff) {
 isStaff = staff;
 }

 public String getComment() {
 return comment;
 }

 public void setComment(String comment) {
 this.comment = comment;
 }

 public long getCreatedAt() {
 return createdAt;
 }

 public void setCreatedAt(long createdAt) {
 this.createdAt = createdAt;
 }

 @Override
 public String toString() {
 return "TicketComment{" +
 "id=" + id +
 ", ticketId=" + ticketId +
 ", authorUuid='" + authorUuid + '\'' +
 ", authorName='" + authorName + '\'' +
 ", isStaff=" + isStaff +
 ", createdAt=" + createdAt +
 '}';
 }
}
