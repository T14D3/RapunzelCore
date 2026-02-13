package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Ticket;
import de.t14d3.rapunzelcore.database.entities.TicketComment;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Commands for the Tickets module.
 * Provides /ticket command with subcommands for ticket management.
 */
public class TicketCommands {
    private final RapunzelCore core;
    private final SpoolDatabase database;
    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketsConfig config;
    private final TicketViewerGUI viewerGUI;

    public TicketCommands(RapunzelCore core, SpoolDatabase database, TicketsConfig config) {
        this.core = core;
        this.database = database;
        this.config = config;
        this.ticketRepository = TicketRepository.getInstance();
        this.commentRepository = TicketCommentRepository.getInstance();
        this.viewerGUI = new TicketViewerGUI();
    }

    public void register() {
        // /ticket create <category> <subject> <description>
        new CommandAPICommand("ticket")
                .withFullDescription("Ticket management commands")
                .withPermission("rapunzelcore.ticket.use")
                .withSubcommand(createSubcommand())
                .withSubcommand(listSubcommand())
                .withSubcommand(viewSubcommand())
                .withSubcommand(commentSubcommand())
                .withSubcommand(assignSubcommand())
                .withSubcommand(statusSubcommand())
                .withSubcommand(resolveSubcommand())
                .withSubcommand(reopenSubcommand())
                .withSubcommand(deleteSubcommand())
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    private CommandAPICommand createSubcommand() {
        return new CommandAPICommand("create")
                .withPermission("rapunzelcore.ticket.create")
                .withArguments(
                        new StringArgument("category")
                                .replaceSuggestions((sender, builder) -> {
                                    config.getCategories().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                )
                .withArguments(new GreedyStringArgument("subject"))
                .withArguments(new GreedyStringArgument("description"))
                .executesPlayer((player, args) -> {
                    String category = (String) args.get("category");
                    String subject = (String) args.get("subject");
                    String description = (String) args.get("description");

                    // Validate category
                    if (!config.getCategories().contains(category)) {
                        player.sendMessage(Component.text("Invalid category. Available: " + String.join(", ", config.getCategories()))
                                .color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check max open tickets
                    ticketRepository.countOpenTicketsAsync(player.getUniqueId()).thenAccept(count -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (count >= config.getMaxOpenTicketsPerPlayer()) {
                                player.sendMessage(Component.text("You have reached the maximum number of open tickets.")
                                        .color(NamedTextColor.RED));
                                return;
                            }

                            // Create ticket
                            String server = RapunzelPaperCore.getServerName();
                            String priority = config.getDefaultPriority();

                            Ticket ticket = new Ticket();
                            ticket.setPlayerUuid(player.getUniqueId().toString());
                            ticket.setPlayerName(player.getName());
                            ticket.setCategory(category);
                            ticket.setPriority(priority);
                            ticket.setSubject(subject);
                            ticket.setDescription(description);
                            ticket.setStatus(TicketStatus.OPEN.name());
                            ticket.setServer(server);
                            ticket.setLocationWorld(player.getWorld().getName());
                            ticket.setLocationX(player.getLocation().getX());
                            ticket.setLocationY(player.getLocation().getY());
                            ticket.setLocationZ(player.getLocation().getZ());
                            ticket.setCreatedAt(System.currentTimeMillis());
                            ticket.setUpdatedAt(System.currentTimeMillis());

                            ticketRepository.createTicketAsync(ticket).thenAccept(created -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (created != null) {
                                        player.sendMessage(Component.text("Ticket " + created.getTicketNumber() + " created successfully!")
                                                .color(NamedTextColor.GREEN));

                                        // Notify staff if enabled
                                        if (config.isNotifyStaffOnCreate()) {
                                            Component notifyMsg = Component.text("[Tickets] " + player.getName() + " created ticket " + created.getTicketNumber())
                                                    .color(NamedTextColor.YELLOW);
                                            Bukkit.broadcast(notifyMsg, "rapunzelcore.ticket.notify");
                                        }
                                    } else {
                                        player.sendMessage(Component.text("Failed to create ticket. Please try again.")
                                                .color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand listSubcommand() {
        return new CommandAPICommand("list")
                .withPermission("rapunzelcore.ticket.list")
                .withOptionalArguments(
                        new StringArgument("filter")
                                .replaceSuggestions((sender, builder) -> {
                                    builder.suggest("player");
                                    builder.suggest("all");
                                    builder.suggest("assigned");
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new StringArgument("status")
                                .replaceSuggestions((sender, builder) -> {
                                    for (TicketStatus status : TicketStatus.values()) {
                                        builder.suggest(status.name());
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((sender, args) -> {
                    String filter = args.get("filter") == null ? "player" : (String) args.get("filter");
                    String statusStr = (String) args.get("status");
                    TicketStatus status = statusStr == null ? null : TicketStatus.valueOf(statusStr);

                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(Component.text("This command can only be used by players.")
                                .color(NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    switch (filter.toLowerCase()) {
                        case "player" -> {
                            // List player's own tickets
                            ticketRepository.listTicketsAsync(player.getUniqueId(), status).thenAccept(tickets -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (tickets.isEmpty()) {
                                        player.sendMessage(Component.text("You have no tickets.").color(NamedTextColor.YELLOW));
                                    } else {
                                        viewerGUI.openTicketList(player, tickets, "Your Tickets");
                                    }
                                });
                            });
                        }
                        case "all" -> {
                            // List all tickets (staff only)
                            if (!player.hasPermission("rapunzelcore.ticket.list.all")) {
                                player.sendMessage(Component.text("You don't have permission to list all tickets.")
                                        .color(NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            ticketRepository.listAllTicketsAsync(status).thenAccept(tickets -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (tickets.isEmpty()) {
                                        player.sendMessage(Component.text("No tickets found.").color(NamedTextColor.YELLOW));
                                    } else {
                                        viewerGUI.openTicketList(player, tickets, "All Tickets");
                                    }
                                });
                            });
                        }
                        case "assigned" -> {
                            // List tickets assigned to player (staff only)
                            if (!player.hasPermission("rapunzelcore.ticket.list.assigned")) {
                                player.sendMessage(Component.text("You don't have permission to list assigned tickets.")
                                        .color(NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }
                            ticketRepository.listAssignedTicketsAsync(player.getUniqueId().toString()).thenAccept(tickets -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (tickets.isEmpty()) {
                                        player.sendMessage(Component.text("You have no assigned tickets.").color(NamedTextColor.YELLOW));
                                    } else {
                                        viewerGUI.openTicketList(player, tickets, "Assigned Tickets");
                                    }
                                });
                            });
                        }
                        default -> player.sendMessage(Component.text("Invalid filter. Use: player, all, assigned")
                                .color(NamedTextColor.RED));
                    }

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand viewSubcommand() {
        return new CommandAPICommand("view")
                .withPermission("rapunzelcore.ticket.view")
                .withArguments(new StringArgument("number"))
                .executesPlayer((player, args) -> {
                    String number = (String) args.get("number");

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                player.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            // Check permissions
                            boolean isOwner = ticket.getPlayerUuid().equals(player.getUniqueId().toString());
                            boolean isAssigned = ticket.getAssignedTo() != null && ticket.getAssignedTo().equals(player.getUniqueId().toString());
                            boolean isStaff = player.hasPermission("rapunzelcore.ticket.view.all");

                            if (!isOwner && !isAssigned && !isStaff) {
                                player.sendMessage(Component.text("You don't have permission to view this ticket.")
                                        .color(NamedTextColor.RED));
                                return;
                            }

                            // Load comments
                            commentRepository.getCommentsAsync(ticket.getId()).thenAccept(comments -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    viewerGUI.openTicketDetail(player, ticket, comments);
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand commentSubcommand() {
        return new CommandAPICommand("comment")
                .withPermission("rapunzelcore.ticket.comment")
                .withArguments(new StringArgument("number"))
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    String number = (String) args.get("number");
                    String message = (String) args.get("message");

                    boolean isStaff = sender.hasPermission("rapunzelcore.ticket.comment.staff");
                    String authorName = sender instanceof Player ? ((Player) sender).getName() : "Console";
                    String authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            // Check permissions
                            boolean isOwner = ticket.getPlayerUuid().equals(authorUuid);
                            if (!isOwner && !isStaff) {
                                sender.sendMessage(Component.text("You don't have permission to comment on this ticket.")
                                        .color(NamedTextColor.RED));
                                return;
                            }

                            TicketComment comment = new TicketComment();
                            comment.setTicketId(ticket.getId());
                            comment.setAuthorUuid(authorUuid);
                            comment.setAuthorName(authorName);
                            comment.setStaff(isStaff);
                            comment.setComment(message);
                            comment.setCreatedAt(System.currentTimeMillis());

                            commentRepository.addCommentAsync(comment).thenAccept(created -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (created != null) {
                                        sender.sendMessage(Component.text("Comment added to ticket " + number)
                                                .color(NamedTextColor.GREEN));

                                        // Update ticket timestamp
                                        ticket.setUpdatedAt(System.currentTimeMillis());
                                        ticketRepository.updateTicketAsync(ticket);
                                    } else {
                                        sender.sendMessage(Component.text("Failed to add comment.").color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand assignSubcommand() {
        return new CommandAPICommand("assign")
                .withPermission("rapunzelcore.ticket.assign")
                .withArguments(new StringArgument("number"))
                .withOptionalArguments(new StringArgument("staff"))
                .executes((sender, args) -> {
                    String number = (String) args.get("number");
                    String staffName = (String) args.get("staff");

                    String assignerName = sender instanceof Player ? ((Player) sender).getName() : "Console";
                    String assignerUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            String targetStaffUuid;
                            String targetStaffName;

                            if (staffName == null) {
                                // Self-assign
                                if (!(sender instanceof Player)) {
                                    sender.sendMessage(Component.text("Console cannot self-assign. Specify a staff member.")
                                            .color(NamedTextColor.RED));
                                    return;
                                }
                                targetStaffUuid = assignerUuid;
                                targetStaffName = assignerName;
                            } else {
                                // Assign to specific staff
                                OfflinePlayer target = Bukkit.getOfflinePlayer(staffName);
                                if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                                    sender.sendMessage(Component.text("Staff member not found.").color(NamedTextColor.RED));
                                    return;
                                }
                                targetStaffUuid = target.getUniqueId().toString();
                                targetStaffName = target.getName();
                            }

                            ticket.setAssignedTo(targetStaffUuid);
                            ticket.setStatus(TicketStatus.IN_PROGRESS.name());
                            ticket.setUpdatedAt(System.currentTimeMillis());

                            ticketRepository.updateTicketAsync(ticket).thenAccept(success -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (success) {
                                        sender.sendMessage(Component.text("Ticket " + number + " assigned to " + targetStaffName)
                                                .color(NamedTextColor.GREEN));
                                    } else {
                                        sender.sendMessage(Component.text("Failed to assign ticket.").color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand statusSubcommand() {
        return new CommandAPICommand("status")
                .withPermission("rapunzelcore.ticket.status")
                .withArguments(new StringArgument("number"))
                .withArguments(
                        new StringArgument("status")
                                .replaceSuggestions((sender, builder) -> {
                                    for (TicketStatus status : TicketStatus.values()) {
                                        builder.suggest(status.name());
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((sender, args) -> {
                    String number = (String) args.get("number");
                    String newStatus = (String) args.get("status");

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            try {
                                TicketStatus status = TicketStatus.valueOf(newStatus);
                                ticket.setStatus(status.name());
                                ticket.setUpdatedAt(System.currentTimeMillis());

                                ticketRepository.updateTicketAsync(ticket).thenAccept(success -> {
                                    Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                        if (success) {
                                            sender.sendMessage(Component.text("Ticket " + number + " status changed to " + status.name())
                                                    .color(NamedTextColor.GREEN));
                                        } else {
                                            sender.sendMessage(Component.text("Failed to update ticket status.").color(NamedTextColor.RED));
                                        }
                                    });
                                });
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(Component.text("Invalid status. Use: OPEN, IN_PROGRESS, RESOLVED, CLOSED")
                                        .color(NamedTextColor.RED));
                            }
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand resolveSubcommand() {
        return new CommandAPICommand("resolve")
                .withPermission("rapunzelcore.ticket.resolve")
                .withArguments(new StringArgument("number"))
                .withArguments(new GreedyStringArgument("resolution"))
                .executes((sender, args) -> {
                    String number = (String) args.get("number");
                    String resolution = (String) args.get("resolution");

                    String resolverName = sender instanceof Player ? ((Player) sender).getName() : "Console";
                    String resolverUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            ticket.setStatus(TicketStatus.RESOLVED.name());
                            ticket.setResolution(resolution);
                            ticket.setResolvedBy(resolverUuid);
                            ticket.setResolvedAt(System.currentTimeMillis());
                            ticket.setUpdatedAt(System.currentTimeMillis());

                            ticketRepository.updateTicketAsync(ticket).thenAccept(success -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (success) {
                                        sender.sendMessage(Component.text("Ticket " + number + " resolved.").color(NamedTextColor.GREEN));

                                        // Notify player
                                        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(ticket.getPlayerUuid()));
                                        if (player.isOnline() && player.getPlayer() != null) {
                                            player.getPlayer().sendMessage(Component.text("Your ticket " + number + " has been resolved!")
                                                    .color(NamedTextColor.GREEN));
                                        }
                                    } else {
                                        sender.sendMessage(Component.text("Failed to resolve ticket.").color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand reopenSubcommand() {
        return new CommandAPICommand("reopen")
                .withPermission("rapunzelcore.ticket.reopen")
                .withArguments(new StringArgument("number"))
                .withArguments(new GreedyStringArgument("reason"))
                .executes((sender, args) -> {
                    String number = (String) args.get("number");
                    String reason = (String) args.get("reason");

                    String reopenerName = sender instanceof Player ? ((Player) sender).getName() : "Console";
                    String reopenerUuid = sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "CONSOLE";

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            if (!ticket.getStatus().equals(TicketStatus.RESOLVED.name()) && !ticket.getStatus().equals(TicketStatus.CLOSED.name())) {
                                sender.sendMessage(Component.text("Only resolved or closed tickets can be reopened.")
                                        .color(NamedTextColor.RED));
                                return;
                            }

                            ticket.setStatus(TicketStatus.OPEN.name());
                            ticket.setUpdatedAt(System.currentTimeMillis());

                            // Add reopen comment
                            TicketComment comment = new TicketComment();
                            comment.setTicketId(ticket.getId());
                            comment.setAuthorUuid(reopenerUuid);
                            comment.setAuthorName(reopenerName);
                            comment.setStaff(sender.hasPermission("rapunzelcore.ticket.reopen.staff"));
                            comment.setComment("[REOPENED] " + reason);
                            comment.setCreatedAt(System.currentTimeMillis());

                            commentRepository.addCommentAsync(comment);

                            ticketRepository.updateTicketAsync(ticket).thenAccept(success -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (success) {
                                        sender.sendMessage(Component.text("Ticket " + number + " reopened.").color(NamedTextColor.GREEN));
                                    } else {
                                        sender.sendMessage(Component.text("Failed to reopen ticket.").color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    private CommandAPICommand deleteSubcommand() {
        return new CommandAPICommand("delete")
                .withPermission("rapunzelcore.ticket.delete")
                .withArguments(new StringArgument("number"))
                .executes((sender, args) -> {
                    String number = (String) args.get("number");

                    ticketRepository.getTicketByNumberAsync(number).thenAccept(ticket -> {
                        Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                            if (ticket == null) {
                                sender.sendMessage(Component.text("Ticket not found.").color(NamedTextColor.RED));
                                return;
                            }

                            ticketRepository.deleteTicketAsync(ticket).thenAccept(success -> {
                                Bukkit.getScheduler().runTask((JavaPlugin) core, () -> {
                                    if (success) {
                                        sender.sendMessage(Component.text("Ticket " + number + " deleted.").color(NamedTextColor.GREEN));
                                    } else {
                                        sender.sendMessage(Component.text("Failed to delete ticket.").color(NamedTextColor.RED));
                                    }
                                });
                            });
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                });
    }

    public void unregister() {
        CommandAPI.unregister("ticket");
    }
}
