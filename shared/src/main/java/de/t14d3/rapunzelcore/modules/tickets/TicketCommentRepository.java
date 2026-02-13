package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.TicketComment;
import de.t14d3.spool.repository.EntityRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository for TicketComment entity operations.
 * Singleton pattern with async database operations.
 */
public class TicketCommentRepository extends EntityRepository<TicketComment> {
 private static volatile TicketCommentRepository instance;
 private static final Object LOCK = new Object();

 private TicketCommentRepository() {
 super(CoreDatabase.getEntityManager(), TicketComment.class);
 }

 /**
 * Get the singleton instance of TicketCommentRepository.
 * @return The repository instance
 */
 public static TicketCommentRepository getInstance() {
 if (instance == null) {
 synchronized (LOCK) {
 if (instance == null) {
 instance = new TicketCommentRepository();
 }
 }
 }
 return instance;
 }

 /**
 * Add a comment to a ticket asynchronously.
 * @param comment The comment to add
 * @return CompletableFuture with the created comment
 */
 public CompletableFuture<TicketComment> addCommentAsync(TicketComment comment) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 save(comment);
 CoreDatabase.getEntityManager().flush();
 return comment;
 }));
 }

 /**
 * Get all comments for a ticket asynchronously.
 * @param ticketId The ticket ID
 * @return CompletableFuture with list of comments
 */
 public CompletableFuture<List<TicketComment>> getCommentsAsync(long ticketId) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 return findBy("ticketId", ticketId).stream()
 .sorted((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()))
 .toList();
 }));
 }

 /**
 * Delete a comment asynchronously.
 * @param comment The comment to delete
 * @return CompletableFuture with success status
 */
 public CompletableFuture<Boolean> deleteCommentAsync(TicketComment comment) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 try {
 delete(comment);
 CoreDatabase.getEntityManager().flush();
 return true;
 } catch (Exception e) {
 return false;
 }
 }));
 }

 /**
 * Delete a comment by ID asynchronously.
 * @param commentId The comment ID
 * @return CompletableFuture with success status
 */
 public CompletableFuture<Boolean> deleteCommentByIdAsync(long commentId) {
 return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
 try {
 deleteById(commentId);
 CoreDatabase.getEntityManager().flush();
 return true;
 } catch (Exception e) {
 return false;
 }
 }));
 }
}
