package de.t14d3.rapunzelcore.modules.portals;

import org.jetbrains.annotations.NotNull;
import java.util.UUID;

/**
 * Functional interface for handling portal actions.
 * Implementations can be registered with a PortalService to handle
 * custom actions when entities interact with portals.
 */
@FunctionalInterface
public interface PortalActionHandler {

    /**
     * Handles a portal action for an entity.
     *
     * @param entityId the UUID of the entity triggering the action
     * @param portal the portal being interacted with
     * @param action the action string (e.g., "message:Hello World")
     * @return true if the action was handled successfully, false otherwise
     */
    boolean handle(@NotNull UUID entityId, @NotNull Portal portal, @NotNull String action);
}
