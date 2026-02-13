package de.t14d3.rapunzelcore.modules.tickets;

import de.t14d3.rapunzellib.config.YamlConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the Tickets module.
 */
public class TicketsConfig {
 private final YamlConfig config;

 public TicketsConfig(YamlConfig config) {
 this.config = config;
 }

 /**
 * Check if the tickets module is enabled.
 * @return true if enabled
 */
 public boolean isEnabled() {
 return config.getBoolean("enabled", true);
 }

 /**
 * Get the list of ticket categories.
 * @return List of categories
 */
 public List<String> getCategories() {
 return config.getStringList("categories", Arrays.asList(
 "BUG", "PLAYER_REPORT", "SUGGESTION", "QUESTION", "OTHER"
 ));
 }

 /**
 * Get the default priority for new tickets.
 * @return Default priority
 */
 public String getDefaultPriority() {
 return config.getString("default-priority", "MEDIUM");
 }

 /**
 * Get the maximum number of open tickets per player.
 * @return Max open tickets
 */
 public int getMaxOpenTicketsPerPlayer() {
 return config.getInt("max-open-tickets-per-player", 5);
 }

 /**
 * Check if staff should be notified when a new ticket is created.
 * @return true if notifications enabled
 */
 public boolean isNotifyStaffOnCreate() {
 return config.getBoolean("notify-staff-on-create", true);
 }

 /**
 * Check if auto-assign is enabled.
 * @return true if auto-assign enabled
 */
 public boolean isAutoAssignEnabled() {
 return config.getBoolean("auto-assign-enabled", false);
 }

 /**
 * Get the raw YamlConfig for direct access.
 * @return The config
 */
 public YamlConfig getRawConfig() {
 return config;
 }
}
