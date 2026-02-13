package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.database.entities.Ticket;
import de.t14d3.rapunzelcore.database.entities.TicketComment;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paper-specific implementation of the TicketsModule.
 * Handles Bukkit/CommandAPI registration for ticket commands.
 */
public class TicketsModule implements Module {
    private final RapunzelCore core;
    private final SpoolDatabase database;
    private TicketsConfig config;
    private TicketCommands ticketCommands;
    private TicketsListener ticketsListener;
    private TicketRepository ticketRepository;
    private TicketCommentRepository commentRepository;
    private boolean enabled = false;

    public TicketsModule(RapunzelCore core, SpoolDatabase database) {
        this.core = core;
        this.database = database;
    }

    @Override
    public String getName() {
        return "tickets";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    @Override
    public void enable(RapunzelCore core) {
        if (enabled) {
            return;
        }

        // Load configuration
        this.config = new TicketsConfig(loadConfig());

        if (!config.isEnabled()) {
            RapunzelCore.getLogger().info("Tickets module is disabled in configuration.");
            return;
        }

        // Initialize repositories
        this.ticketRepository = TicketRepository.getInstance();
        this.commentRepository = TicketCommentRepository.getInstance();

        // Register commands
        this.ticketCommands = new TicketCommands(core, database, config);
        this.ticketCommands.register();

        // Register listener
        this.ticketsListener = new TicketsListener(this);
        ((JavaPlugin) core).getServer().getPluginManager().registerEvents(ticketsListener, (JavaPlugin) core);

        this.enabled = true;
        RapunzelCore.getLogger().info("Tickets module enabled successfully.");
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }

        // Unregister commands
        if (ticketCommands != null) {
            ticketCommands.unregister();
        }

        // Unregister listener
        if (ticketsListener != null) {
            ticketsListener.unregister();
        }

        this.enabled = false;
        RapunzelCore.getLogger().info("Tickets module disabled.");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public TicketRepository getTicketRepository() {
        return ticketRepository;
    }

    public TicketCommentRepository getTicketCommentRepository() {
        return commentRepository;
    }

    public CompletableFuture<Ticket> createTicket(
            UUID playerUuid,
            String playerName,
            String category,
            String priority,
            String subject,
            String description,
            String server,
            String locationWorld,
            double locationX,
            double locationY,
            double locationZ
    ) {
        Ticket ticket = new Ticket();
        ticket.setPlayerUuid(playerUuid.toString());
        ticket.setPlayerName(playerName);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setStatus(TicketStatus.OPEN.name());
        ticket.setServer(server);
        ticket.setLocationWorld(locationWorld);
        ticket.setLocationX(locationX);
        ticket.setLocationY(locationY);
        ticket.setLocationZ(locationZ);
        ticket.setCreatedAt(System.currentTimeMillis());
        ticket.setUpdatedAt(System.currentTimeMillis());

        return ticketRepository.createTicketAsync(ticket);
    }

    public CompletableFuture<Boolean> assignTicket(String ticketNumber, UUID staffUuid, String staffName) {
        return ticketRepository.getTicketByNumberAsync(ticketNumber).thenCompose(ticket -> {
            if (ticket == null) {
                return CompletableFuture.completedFuture(false);
            }

            ticket.setAssignedTo(staffUuid != null ? staffUuid.toString() : null);
            ticket.setStatus(TicketStatus.IN_PROGRESS.name());
            ticket.setUpdatedAt(System.currentTimeMillis());

            return ticketRepository.updateTicketAsync(ticket);
        });
    }

    public CompletableFuture<Boolean> resolveTicket(String ticketNumber, String resolution, UUID resolvedByUuid, String resolvedByName) {
        return ticketRepository.getTicketByNumberAsync(ticketNumber).thenCompose(ticket -> {
            if (ticket == null) {
                return CompletableFuture.completedFuture(false);
            }

            ticket.setStatus(TicketStatus.RESOLVED.name());
            ticket.setResolution(resolution);
            ticket.setResolvedBy(resolvedByUuid.toString());
            ticket.setResolvedAt(System.currentTimeMillis());
            ticket.setUpdatedAt(System.currentTimeMillis());

            return ticketRepository.updateTicketAsync(ticket);
        });
    }

    public CompletableFuture<Boolean> closeTicket(String ticketNumber, UUID closedByUuid, String closedByName) {
        return ticketRepository.getTicketByNumberAsync(ticketNumber).thenCompose(ticket -> {
            if (ticket == null) {
                return CompletableFuture.completedFuture(false);
            }

            ticket.setStatus(TicketStatus.CLOSED.name());
            ticket.setUpdatedAt(System.currentTimeMillis());

            return ticketRepository.updateTicketAsync(ticket);
        });
    }

    public CompletableFuture<Boolean> reopenTicket(String ticketNumber, String reason, UUID reopenedByUuid, String reopenedByName) {
        return ticketRepository.getTicketByNumberAsync(ticketNumber).thenCompose(ticket -> {
            if (ticket == null) {
                return CompletableFuture.completedFuture(false);
            }

            ticket.setStatus(TicketStatus.OPEN.name());
            ticket.setUpdatedAt(System.currentTimeMillis());

            // Add reopen comment
            TicketComment comment = new TicketComment();
            comment.setTicketId(ticket.getId());
            comment.setAuthorUuid(reopenedByUuid.toString());
            comment.setAuthorName(reopenedByName);
            comment.setStaff(true);
            comment.setComment("[REOPENED] " + reason);
            comment.setCreatedAt(System.currentTimeMillis());

            commentRepository.addCommentAsync(comment);

            return ticketRepository.updateTicketAsync(ticket);
        });
    }

    public CompletableFuture<TicketComment> addComment(
            long ticketId,
            UUID authorUuid,
            String authorName,
            boolean isStaff,
            String comment
    ) {
        TicketComment ticketComment = new TicketComment();
        ticketComment.setTicketId(ticketId);
        ticketComment.setAuthorUuid(authorUuid.toString());
        ticketComment.setAuthorName(authorName);
        ticketComment.setStaff(isStaff);
        ticketComment.setComment(comment);
        ticketComment.setCreatedAt(System.currentTimeMillis());

        return commentRepository.addCommentAsync(ticketComment);
    }

    /**
     * Get the tickets configuration.
     *
     * @return The configuration
     */
    public TicketsConfig getConfig() {
        return config;
    }

    /**
     * Get the tickets listener.
     *
     * @return The listener
     */
    public TicketsListener getTicketsListener() {
        return ticketsListener;
    }
}
