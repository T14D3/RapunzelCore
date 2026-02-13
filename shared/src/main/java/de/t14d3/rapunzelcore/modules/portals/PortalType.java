package de.t14d3.rapunzelcore.modules.portals;

/**
 * Types of portals with different behaviors.
 * Simplified to only two core types: teleport and command.
 * Other behaviors (delayed, conditional, cross-server) are handled via action parameters.
 */
public enum PortalType {
    /**
     * Teleport portal - teleports entities to a specific location.
     * Can include cross-server transfer via targetServer field.
     */
    TELEPORT,

    /**
     * Command portal - executes commands when entities enter.
     */
    COMMAND
}
