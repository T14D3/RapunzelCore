package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Ticket;
import de.t14d3.spool.repository.EntityRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repository for Ticket entity operations.
 * Singleton pattern with async database operations.
 */
public class TicketRepository extends EntityRepository<Ticket> {
    private static volatile TicketRepository instance;
    private static final Object LOCK = new Object();
    private final AtomicInteger ticketCounter = new AtomicInteger(0);

    private TicketRepository() {
        super(CoreDatabase.getEntityManager(), Ticket.class);
    }

    /**
     * Get the singleton instance of TicketRepository.
     *
     * @return The repository instance
     */
    public static TicketRepository getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new TicketRepository();
                }
            }
        }
        return instance;
    }

    /**
     * Create a new ticket asynchronously.
     *
     * @param ticket The ticket to create
     * @return CompletableFuture with the created ticket
     */
    public CompletableFuture<Ticket> createTicketAsync(Ticket ticket) {
        return CoreDatabase.supplyAsync(() -> {
            return CoreDatabase.locked(() -> {
                ticket.setTicketNumber(generateTicketNumber());
                save(ticket);
                CoreDatabase.getEntityManager().flush();
                return ticket;
            });
        });
    }

    /**
     * Get a ticket by ID asynchronously.
     *
     * @param id The ticket ID
     * @return CompletableFuture with the ticket or null
     */
    public CompletableFuture<Ticket> getTicketAsync(long id) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> findById(id)));
    }

    /**
     * Get a ticket by ticket number asynchronously.
     *
     * @param ticketNumber The ticket number
     * @return CompletableFuture with the ticket or null
     */
    public CompletableFuture<Ticket> getTicketByNumberAsync(String ticketNumber) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> findOneBy("ticketNumber", ticketNumber)));
    }

    /**
     * List tickets for a player with optional status filter.
     *
     * @param playerUuid The player UUID (null for all players)
     * @param status     The status filter (null for all statuses)
     * @return CompletableFuture with list of tickets
     */
    public CompletableFuture<List<Ticket>> listTicketsAsync(UUID playerUuid, TicketStatus status) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            List<Ticket> tickets = findAll();
            return tickets.stream()
                    .filter(t -> playerUuid == null || t.getPlayerUuid().equals(playerUuid.toString()))
                    .filter(t -> status == null || t.getStatus().equals(status.name()))
                    .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                    .toList();
        }));
    }

    /**
     * List all tickets with optional status filter.
     *
     * @param status The status filter (null for all statuses)
     * @return CompletableFuture with list of tickets
     */
    public CompletableFuture<List<Ticket>> listAllTicketsAsync(TicketStatus status) {
        return listTicketsAsync(null, status);
    }

    /**
     * List tickets assigned to a staff member.
     *
     * @param staffUuid The staff UUID
     * @return CompletableFuture with list of tickets
     */
    public CompletableFuture<List<Ticket>> listAssignedTicketsAsync(String staffUuid) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            List<Ticket> tickets = findAll();
            return tickets.stream()
                    .filter(t -> staffUuid.equals(t.getAssignedTo()))
                    .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                    .toList();
        }));
    }

    /**
     * Update a ticket asynchronously.
     *
     * @param ticket The ticket to update
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> updateTicketAsync(Ticket ticket) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            try {
                ticket.setUpdatedAt(System.currentTimeMillis());
                save(ticket);
                CoreDatabase.getEntityManager().flush();
                return true;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    /**
     * Delete a ticket asynchronously.
     *
     * @param ticket The ticket to delete
     * @return CompletableFuture with success status
     */
    public CompletableFuture<Boolean> deleteTicketAsync(Ticket ticket) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            try {
                delete(ticket);
                CoreDatabase.getEntityManager().flush();
                return true;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    /**
     * Generate a unique ticket number.
     * Format: T-XXXXXX (6 digits with leading zeros)
     *
     * @return The generated ticket number
     */
    public String generateTicketNumber() {
        long count = CoreDatabase.locked(this::count);
        return String.format("T-%06d", count + 1);
    }

    /**
     * Count open tickets for a player.
     *
     * @param playerUuid The player UUID
     * @return CompletableFuture with the count
     */
    public CompletableFuture<Long> countOpenTicketsAsync(UUID playerUuid) {
        return CoreDatabase.supplyAsync(() -> CoreDatabase.locked(() -> {
            return findAll().stream()
                    .filter(t -> t.getPlayerUuid().equals(playerUuid.toString()))
                    .filter(t -> !t.getStatus().equals(TicketStatus.RESOLVED.name()))
                    .filter(t -> !t.getStatus().equals(TicketStatus.CLOSED.name()))
                    .count();
        }));
    }
}
