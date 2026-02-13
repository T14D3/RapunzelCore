package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.RapunzelCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Listener for ticket-related events.
 * Handles notifications for new tickets and updates.
 */
public class TicketsListener implements Listener {
 private final TicketsModule module;

 public TicketsListener(TicketsModule module) {
 this.module = module;
 }

 /**
 * Notify staff when a new ticket is created.
 * @param ticketNumber The ticket number
 * @param playerName The player who created the ticket
 */
 public void notifyStaffNewTicket(String ticketNumber, String playerName) {
 if (!module.getConfig().isNotifyStaffOnCreate()) {
 return;
 }

 Component message = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text(playerName)
 .color(NamedTextColor.YELLOW))
 .append(Component.text(" created ticket ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(ticketNumber)
 .color(NamedTextColor.GREEN))
 .append(Component.text(" - Click to view")
 .color(NamedTextColor.GRAY));

 Bukkit.broadcast(message, "rapunzelcore.ticket.notify");
 }

 /**
 * Notify a player when their ticket is updated.
 * @param playerUuid The player UUID
 * @param ticketNumber The ticket number
 * @param message The update message
 */
 public void notifyPlayerTicketUpdate(UUID playerUuid, String ticketNumber, String message) {
 Player player = Bukkit.getPlayer(playerUuid);
 if (player != null && player.isOnline()) {
 Component msg = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text("Your ticket ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(ticketNumber)
 .color(NamedTextColor.GREEN))
 .append(Component.text(": " + message)
 .color(NamedTextColor.YELLOW));

 player.sendMessage(msg);
 }
 }

 /**
 * Notify a player when their ticket is resolved.
 * @param playerUuid The player UUID
 * @param ticketNumber The ticket number
 * @param resolverName The name of who resolved it
 */
 public void notifyPlayerTicketResolved(UUID playerUuid, String ticketNumber, String resolverName) {
 Player player = Bukkit.getPlayer(playerUuid);
 if (player != null && player.isOnline()) {
 Component msg = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text("Your ticket ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(ticketNumber)
 .color(NamedTextColor.GREEN))
 .append(Component.text(" has been resolved by ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(resolverName)
 .color(NamedTextColor.YELLOW))
 .append(Component.text("!").color(NamedTextColor.WHITE));

 player.sendMessage(msg);
 }
 }

 /**
 * Notify a player when their ticket is assigned.
 * @param playerUuid The player UUID
 * @param ticketNumber The ticket number
 * @param assigneeName The name of who it's assigned to
 */
 public void notifyPlayerTicketAssigned(UUID playerUuid, String ticketNumber, String assigneeName) {
 Player player = Bukkit.getPlayer(playerUuid);
 if (player != null && player.isOnline()) {
 Component msg = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text("Your ticket ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(ticketNumber)
 .color(NamedTextColor.GREEN))
 .append(Component.text(" has been assigned to ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(assigneeName)
 .color(NamedTextColor.YELLOW));

 player.sendMessage(msg);
 }
 }

 /**
 * Notify staff member when a ticket is assigned to them.
 * @param staffUuid The staff UUID
 * @param ticketNumber The ticket number
 * @param playerName The player who created the ticket
 */
 public void notifyStaffAssigned(UUID staffUuid, String ticketNumber, String playerName) {
 Player staff = Bukkit.getPlayer(staffUuid);
 if (staff != null && staff.isOnline()) {
 Component msg = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text("Ticket ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(ticketNumber)
 .color(NamedTextColor.GREEN))
 .append(Component.text(" from ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(playerName)
 .color(NamedTextColor.YELLOW))
 .append(Component.text(" has been assigned to you.")
 .color(NamedTextColor.WHITE));

 staff.sendMessage(msg);
 }
 }

 @EventHandler
 public void onPlayerJoin(PlayerJoinEvent event) {
 Player player = event.getPlayer();

 // Check if player has permission to view assigned tickets
 if (!player.hasPermission("rapunzelcore.ticket.notify")) {
 return;
 }

 // Check for assigned tickets
 module.getTicketRepository().listAssignedTicketsAsync(player.getUniqueId().toString())
 .thenAccept(tickets -> {
 Bukkit.getScheduler().runTask((JavaPlugin) RapunzelCore.getInstance(), () -> {
 long openCount = tickets.stream()
 .filter(t -> !t.getStatus().equals(TicketStatus.RESOLVED.name()))
 .filter(t -> !t.getStatus().equals(TicketStatus.CLOSED.name()))
 .count();

 if (openCount > 0) {
 Component msg = Component.text("[Tickets] ")
 .color(NamedTextColor.GOLD)
 .append(Component.text("You have ")
 .color(NamedTextColor.WHITE))
 .append(Component.text(String.valueOf(openCount))
 .color(NamedTextColor.YELLOW))
 .append(Component.text(" assigned ticket(s). Use /ticket list assigned")
 .color(NamedTextColor.WHITE));

 player.sendMessage(msg);
 }
 });
 });
 }

 /**
 * Unregister this listener.
 */
 public void unregister() {
 HandlerList.unregisterAll(this);
 }
}
