package de.t14d3.rapunzelcore.modules.portals;

import de.t14d3.rapunzellib.events.CancellablePreEvent;
import de.t14d3.rapunzellib.events.Decision;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Event fired when an entity enters a portal.
 * This event is cancellable to prevent the portal action from executing.
 */
public class PortalEntryEvent implements CancellablePreEvent {

    private final UUID entity;
    private final Portal portal;
    private final boolean crossServerTransfer;
    private Decision decision;
    private Component denyReason;
    private final boolean platformCancelled;

    /**
     * Creates a new PortalEntryEvent.
     *
     * @param entity the UUID of the entity entering the portal
     * @param portal the portal being entered
     * @param crossServerTransfer whether this is a cross-server transfer
     */
    public PortalEntryEvent(@NotNull UUID entity, @NotNull Portal portal, boolean crossServerTransfer) {
        this(entity, portal, crossServerTransfer, false);
    }

    /**
     * Creates a new PortalEntryEvent with platform cancellation state.
     *
     * @param entity the UUID of the entity entering the portal
     * @param portal the portal being entered
     * @param crossServerTransfer whether this is a cross-server transfer
     * @param platformCancelled whether the platform event was already cancelled
     */
    public PortalEntryEvent(@NotNull UUID entity, @NotNull Portal portal, boolean crossServerTransfer, boolean platformCancelled) {
        this.entity = entity;
        this.portal = portal;
        this.crossServerTransfer = crossServerTransfer;
        this.decision = Decision.PASS;
        this.denyReason = null;
        this.platformCancelled = platformCancelled;
    }

    /**
     * Gets the UUID of the entity entering the portal.
     *
     * @return the entity UUID
     */
    @NotNull
    public UUID getEntity() {
        return entity;
    }

    /**
     * Gets the portal being entered.
     *
     * @return the portal
     */
    @NotNull
    public Portal getPortal() {
        return portal;
    }

    /**
     * Checks if this is a cross-server transfer.
     *
     * @return true if transferring to another server
     */
    public boolean isCrossServerTransfer() {
        return crossServerTransfer;
    }

    @Override
    public Decision decision() {
        return decision;
    }

    @Override
    public boolean isCancelled() {
        return platformCancelled || decision == Decision.DENY;
    }

    /**
     * Legacy method for checking cancellation state.
     *
     * @return true if the event is cancelled
     */
    public boolean isCancelledLegacy() {
        return isCancelled();
    }

    /**
     * Legacy method for setting cancellation state.
     * This will set the decision to DENY if true, or PASS if false.
     *
     * @param cancelled whether to cancel the event
     */
    public void setCancelled(boolean cancelled) {
        if (cancelled) {
            deny();
        } else {
            pass();
        }
    }

    @Override
    public void pass() {
        this.decision = Decision.PASS;
    }

    @Override
    public void allow() {
        this.decision = Decision.ALLOW;
    }

    @Override
    public void deny() {
        this.decision = Decision.DENY;
    }

    @Override
    public void deny(@Nullable Component reason) {
        this.decision = Decision.DENY;
        this.denyReason = reason;
    }

    @Override
    @NotNull
    public Optional<Component> denyReason() {
        return Optional.ofNullable(denyReason);
    }

    @Override
    public String toString() {
        return "PortalEntryEvent{" +
               "entity=" + entity +
               ", portal=" + portal.id() +
               ", crossServerTransfer=" + crossServerTransfer +
               ", decision=" + decision +
               ", cancelled=" + isCancelled() +
               '}';
    }
}
