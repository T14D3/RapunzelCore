package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzelcore.database.entities.Ticket;
import de.t14d3.rapunzelcore.database.entities.TicketComment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * GUI for viewing tickets and ticket details.
 * Provides paginated list and detail views.
 */
public class TicketViewerGUI {
 private static final int ITEMS_PER_PAGE = 45;
 private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

 /**
 * Open a paginated list of tickets.
 * @param viewer The player viewing
 * @param tickets The list of tickets
 * @param title The GUI title
 */
 public void openTicketList(Player viewer, List<Ticket> tickets, String title) {
 openTicketListPage(viewer, tickets, title, 0);
 }

 private void openTicketListPage(Player viewer, List<Ticket> tickets, String title, int page) {
 int totalPages = (int) Math.ceil(tickets.size() / (double) ITEMS_PER_PAGE);
 String pageTitle = title + " (" + (page + 1) + "/" + Math.max(1, totalPages) + ")";

 Inventory gui = Bukkit.createInventory(new TicketListHolder(page, tickets, title), 54, Component.text(pageTitle));

 // Add ticket items
 int startIndex = page * ITEMS_PER_PAGE;
 int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, tickets.size());

 for (int i = startIndex; i < endIndex; i++) {
 Ticket ticket = tickets.get(i);
 ItemStack item = createTicketItem(ticket);
 gui.setItem(i - startIndex, item);
 }

 // Navigation buttons
 if (page > 0) {
 gui.setItem(45, createNavigationItem(Material.ARROW, "Previous Page"));
 }
 if (page < totalPages - 1) {
 gui.setItem(53, createNavigationItem(Material.ARROW, "Next Page"));
 }

 // Close button
 gui.setItem(49, createNavigationItem(Material.BARRIER, "Close"));

 viewer.openInventory(gui);
 }

 private ItemStack createTicketItem(Ticket ticket) {
 Material material = switch (TicketStatus.valueOf(ticket.getStatus())) {
 case OPEN -> Material.LIME_WOOL;
 case IN_PROGRESS -> Material.YELLOW_WOOL;
 case RESOLVED -> Material.BLUE_WOOL;
 case CLOSED -> Material.RED_WOOL;
 };

 ItemStack item = new ItemStack(material);
 ItemMeta meta = item.getItemMeta();

 meta.displayName(Component.text("#" + ticket.getTicketNumber() + " - " + ticket.getSubject())
 .color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));

 List<Component> lore = new ArrayList<>();
 lore.add(Component.text("Player: " + ticket.getPlayerName()).color(NamedTextColor.GRAY));
 lore.add(Component.text("Category: " + ticket.getCategory()).color(NamedTextColor.GRAY));
 lore.add(Component.text("Priority: " + ticket.getPriority()).color(getPriorityColor(ticket.getPriority())));
 lore.add(Component.text("Status: " + ticket.getStatus()).color(getStatusColor(ticket.getStatus())));

 if (ticket.getAssignedTo() != null) {
 lore.add(Component.text("Assigned to: " + ticket.getAssignedTo()).color(NamedTextColor.GRAY));
 }

 lore.add(Component.text("Server: " + ticket.getServer()).color(NamedTextColor.GRAY));
 lore.add(Component.text("Created: " + DATE_FORMAT.format(new Date(ticket.getCreatedAt()))).color(NamedTextColor.GRAY));
 lore.add(Component.empty());
 lore.add(Component.text("Click to view details").color(NamedTextColor.YELLOW));

 meta.lore(lore);
 item.setItemMeta(meta);

 return item;
 }

 private NamedTextColor getPriorityColor(String priority) {
 return switch (priority.toUpperCase()) {
 case "CRITICAL" -> NamedTextColor.DARK_RED;
 case "HIGH" -> NamedTextColor.RED;
 case "MEDIUM" -> NamedTextColor.YELLOW;
 case "LOW" -> NamedTextColor.GREEN;
 default -> NamedTextColor.GRAY;
 };
 }

 private NamedTextColor getStatusColor(String status) {
 return switch (TicketStatus.valueOf(status)) {
 case OPEN -> NamedTextColor.GREEN;
 case IN_PROGRESS -> NamedTextColor.YELLOW;
 case RESOLVED -> NamedTextColor.BLUE;
 case CLOSED -> NamedTextColor.RED;
 };
 }

 private ItemStack createNavigationItem(Material material, String name) {
 ItemStack item = new ItemStack(material);
 ItemMeta meta = item.getItemMeta();
 meta.displayName(Component.text(name).color(NamedTextColor.WHITE));
 item.setItemMeta(meta);
 return item;
 }

 /**
 * Open a detailed view of a ticket with comments.
 * @param viewer The player viewing
 * @param ticket The ticket to view
 * @param comments The list of comments
 */
 public void openTicketDetail(Player viewer, Ticket ticket, List<TicketComment> comments) {
 String title = "Ticket #" + ticket.getTicketNumber();
 Inventory gui = Bukkit.createInventory(new TicketDetailHolder(ticket), 54, Component.text(title));

 // Ticket info item
 gui.setItem(4, createTicketInfoItem(ticket));

 // Location item
 gui.setItem(19, createLocationItem(ticket));

 // Status item
 gui.setItem(20, createStatusItem(ticket));

 // Priority item
 gui.setItem(21, createPriorityItem(ticket));

 // Assignment item
 gui.setItem(22, createAssignmentItem(ticket));

 // Comments section (starting at row 4)
 int commentSlot = 36;
 for (TicketComment comment : comments) {
 if (commentSlot >= 53) break;
 gui.setItem(commentSlot++, createCommentItem(comment));
 }

 // Back button
 gui.setItem(49, createNavigationItem(Material.ARROW, "Back to List"));

 // Close button
 gui.setItem(52, createNavigationItem(Material.BARRIER, "Close"));

 viewer.openInventory(gui);
 }

 private ItemStack createTicketInfoItem(Ticket ticket) {
 ItemStack item = new ItemStack(Material.BOOK);
 ItemMeta meta = item.getItemMeta();

 meta.displayName(Component.text("#" + ticket.getTicketNumber() + " - " + ticket.getSubject())
 .color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));

 List<Component> lore = new ArrayList<>();
 lore.add(Component.text("Player: " + ticket.getPlayerName()).color(NamedTextColor.GRAY));
 lore.add(Component.text("Category: " + ticket.getCategory()).color(NamedTextColor.GRAY));
 lore.add(Component.text("Created: " + DATE_FORMAT.format(new Date(ticket.getCreatedAt()))).color(NamedTextColor.GRAY));
 lore.add(Component.empty());
 lore.add(Component.text("Description:").color(NamedTextColor.YELLOW));

 // Split description into lines
 String[] words = ticket.getDescription().split(" ");
 StringBuilder line = new StringBuilder();
 for (String word : words) {
 if (line.length() + word.length() > 30) {
 lore.add(Component.text(line.toString()).color(NamedTextColor.GRAY));
 line = new StringBuilder();
 }
 line.append(word).append(" ");
 }
 if (line.length() > 0) {
 lore.add(Component.text(line.toString()).color(NamedTextColor.GRAY));
 }

 if (ticket.getResolution() != null) {
 lore.add(Component.empty());
 lore.add(Component.text("Resolution:").color(NamedTextColor.GREEN));
 String[] resWords = ticket.getResolution().split(" ");
 StringBuilder resLine = new StringBuilder();
 for (String word : resWords) {
 if (resLine.length() + word.length() > 30) {
 lore.add(Component.text(resLine.toString()).color(NamedTextColor.GRAY));
 resLine = new StringBuilder();
 }
 resLine.append(word).append(" ");
 }
 if (resLine.length() > 0) {
 lore.add(Component.text(resLine.toString()).color(NamedTextColor.GRAY));
 }
 }

 meta.lore(lore);
 item.setItemMeta(meta);
 return item;
 }

 private ItemStack createLocationItem(Ticket ticket) {
 ItemStack item = new ItemStack(Material.COMPASS);
 ItemMeta meta = item.getItemMeta();

 meta.displayName(Component.text("Location").color(NamedTextColor.AQUA));

 List<Component> lore = new ArrayList<>();
 lore.add(Component.text("World: " + ticket.getLocationWorld()).color(NamedTextColor.GRAY));
 lore.add(Component.text("X: " + String.format("%.2f", ticket.getLocationX())).color(NamedTextColor.GRAY));
 lore.add(Component.text("Y: " + String.format("%.2f", ticket.getLocationY())).color(NamedTextColor.GRAY));
 lore.add(Component.text("Z: " + String.format("%.2f", ticket.getLocationZ())).color(NamedTextColor.GRAY));
 lore.add(Component.empty());
 lore.add(Component.text("Server: " + ticket.getServer()).color(NamedTextColor.GRAY));

 meta.lore(lore);
 item.setItemMeta(meta);
 return item;
 }

 private ItemStack createStatusItem(Ticket ticket) {
 Material material = switch (TicketStatus.valueOf(ticket.getStatus())) {
 case OPEN -> Material.LIME_DYE;
 case IN_PROGRESS -> Material.YELLOW_DYE;
 case RESOLVED -> Material.BLUE_DYE;
 case CLOSED -> Material.RED_DYE;
 };

 ItemStack item = new ItemStack(material);
 ItemMeta meta = item.getItemMeta();

 meta.displayName(Component.text("Status: " + ticket.getStatus()).color(getStatusColor(ticket.getStatus())));

 List<Component> lore = new ArrayList<>();
 lore.add(Component.text("Last updated: " + DATE_FORMAT.format(new Date(ticket.getUpdatedAt()))).color(NamedTextColor.GRAY));

 if (ticket.getResolvedAt() != null) {
 lore.add(Component.text("Resolved: " + DATE_FORMAT.format(new Date(ticket.getResolvedAt()))).color(NamedTextColor.GRAY));
 }

 meta.lore(lore);
 item.setItemMeta(meta);
 return item;
 }

 private ItemStack createPriorityItem(Ticket ticket) {
 Material material = switch (ticket.getPriority().toUpperCase()) {
 case "CRITICAL" -> Material.RED_WOOL;
 case "HIGH" -> Material.ORANGE_WOOL;
 case "MEDIUM" -> Material.YELLOW_WOOL;
 case "LOW" -> Material.GREEN_WOOL;
 default -> Material.WHITE_WOOL;
 };

 ItemStack item = new ItemStack(material);
 ItemMeta meta = item.getItemMeta();

 meta.displayName(Component.text("Priority: " + ticket.getPriority()).color(getPriorityColor(ticket.getPriority())));
 item.setItemMeta(meta);
 return item;
 }

 private ItemStack createAssignmentItem(Ticket ticket) {
 ItemStack item = new ItemStack(Material.PLAYER_HEAD);
 ItemMeta meta = item.getItemMeta();

 if (ticket.getAssignedTo() != null) {
 meta.displayName(Component.text("Assigned to: " + ticket.getAssignedTo()).color(NamedTextColor.GREEN));
 } else {
 meta.displayName(Component.text("Unassigned").color(NamedTextColor.RED));
 }

 item.setItemMeta(meta);
 return item;
 }

 private ItemStack createCommentItem(TicketComment comment) {
 Material material = comment.isStaff() ? Material.PAPER : Material.BOOK;
 ItemStack item = new ItemStack(material);
 ItemMeta meta = item.getItemMeta();

 NamedTextColor authorColor = comment.isStaff() ? NamedTextColor.GOLD : NamedTextColor.GRAY;
 meta.displayName(Component.text(comment.getAuthorName()).color(authorColor));

 List<Component> lore = new ArrayList<>();
 lore.add(Component.text(DATE_FORMAT.format(new Date(comment.getCreatedAt()))).color(NamedTextColor.DARK_GRAY));
 lore.add(Component.empty());

 // Split comment into lines
 String[] words = comment.getComment().split(" ");
 StringBuilder line = new StringBuilder();
 for (String word : words) {
 if (line.length() + word.length() > 30) {
 lore.add(Component.text(line.toString()).color(NamedTextColor.GRAY));
 line = new StringBuilder();
 }
 line.append(word).append(" ");
 }
 if (line.length() > 0) {
 lore.add(Component.text(line.toString()).color(NamedTextColor.GRAY));
 }

 meta.lore(lore);
 item.setItemMeta(meta);
 return item;
 }

 /**
 * Inventory holder for ticket list GUI.
 */
 public static class TicketListHolder implements InventoryHolder {
 private final int page;
 private final List<Ticket> tickets;
 private final String title;

 public TicketListHolder(int page, List<Ticket> tickets, String title) {
 this.page = page;
 this.tickets = tickets;
 this.title = title;
 }

 @Override
 public Inventory getInventory() {
 return null;
 }

 public int getPage() {
 return page;
 }

 public List<Ticket> getTickets() {
 return tickets;
 }

 public String getTitle() {
 return title;
 }
 }

 /**
 * Inventory holder for ticket detail GUI.
 */
 public static class TicketDetailHolder implements InventoryHolder {
 private final Ticket ticket;

 public TicketDetailHolder(Ticket ticket) {
 this.ticket = ticket;
 }

 @Override
 public Inventory getInventory() {
 return null;
 }

 public Ticket getTicket() {
 return ticket;
 }
 }
}
