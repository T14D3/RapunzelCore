package de.t14d3.rapunzelcore.modules.portals;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit event fired when an entity enters a portal.
 * Wraps the core PortalEntryEvent for Paper platform.
 */
public class PaperPortalEntryEvent extends Event {

 private static final HandlerList HANDLERS = new HandlerList();
 private final PortalEntryEvent coreEvent;

 public PaperPortalEntryEvent(PortalEntryEvent coreEvent) {
 this.coreEvent = coreEvent;
 }

 public PortalEntryEvent getCoreEvent() {
 return coreEvent;
 }

 public boolean isCancelled() {
 return coreEvent.isCancelled();
 }

 public void setCancelled(boolean cancelled) {
 coreEvent.setCancelled(cancelled);
 }

 @Override
 @NotNull
 public HandlerList getHandlers() {
 return HANDLERS;
 }

 @NotNull
 public static HandlerList getHandlerList() {
 return HANDLERS;
 }
}
