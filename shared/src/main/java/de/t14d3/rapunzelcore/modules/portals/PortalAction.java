package de.t14d3.rapunzelcore.modules.portals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents an action that can be executed when an entity enters a portal.
 * Simplified to two core action types: teleport and command.
 */
public sealed interface PortalAction {

    /**
     * Gets the type of this portal action as a string.
     *
     * @return the action type ("teleport" or "command")
     */
    @NotNull String type();

    /**
     * Gets the priority of this action for ordering when multiple actions exist.
     * Higher priority actions execute first.
     *
     * @return the priority value
     */
    default int priority() {
        return 0;
    }

    /**
     * Gets whether this action requires confirmation before execution.
     *
     * @return true if confirmation is required
     */
    default boolean requiresConfirmation() {
        return false;
    }

    /**
     * Gets the delay in ticks before this action executes.
     *
     * @return delay in ticks, 0 for immediate
     */
    default long delayTicks() {
        return 0;
    }

    /**
     * Gets additional metadata for this action.
     *
     * @return metadata map
     */
    @NotNull Map<String, String> metadata();

    /**
     * Teleport action - moves the entity to a specific location.
     * If targetServer is provided, cross-server transfer is performed.
     */
    record TeleportAction(
        @NotNull String targetWorld,
        double targetX,
        double targetY,
        double targetZ,
        float targetYaw,
        float targetPitch,
        @Nullable String targetServer,
        int priority,
        long delayTicks,
        @NotNull Map<String, String> metadata
    ) implements PortalAction {

        public TeleportAction {
            metadata = Map.copyOf(metadata);
        }

        @Override
        public @NotNull String type() {
            return "teleport";
        }

        public TeleportAction(@NotNull String targetWorld, double x, double y, double z) {
            this(targetWorld, x, y, z, 0.0f, 0.0f, null, 0, 0, Map.of());
        }

        public TeleportAction(@NotNull String targetWorld, double x, double y, double z, float yaw, float pitch) {
            this(targetWorld, x, y, z, yaw, pitch, null, 0, 0, Map.of());
        }

        public TeleportAction(@NotNull String targetServer, @NotNull String targetWorld,
                              double x, double y, double z) {
            this(targetWorld, x, y, z, 0.0f, 0.0f, targetServer, 0, 0, Map.of());
        }
    }

    /**
     * Command action - executes commands as console or as the entity.
     */
    record CommandAction(
        @NotNull List<String> commands,
        boolean executeAsConsole,
        boolean executeAsEntity,
        int priority,
        long delayTicks,
        @NotNull Map<String, String> metadata
    ) implements PortalAction {

        public CommandAction {
            commands = List.copyOf(commands);
            metadata = Map.copyOf(metadata);
        }

        @Override
        public @NotNull String type() {
            return "command";
        }

        public CommandAction(@NotNull String command) {
            this(List.of(command), true, false, 0, 0, Map.of());
        }

        public CommandAction(@NotNull List<String> commands) {
            this(commands, true, false, 0, 0, Map.of());
        }

        public CommandAction(@NotNull String command, boolean executeAsConsole, boolean executeAsEntity) {
            this(List.of(command), executeAsConsole, executeAsEntity, 0, 0, Map.of());
        }
    }
}
