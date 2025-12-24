package de.t14d3.rapunzelcore;

/**
 * Enum representing the environment/platform where a module can run.
 * Modules can be specific to Paper, Velocity, or both platforms.
 */
public enum Environment {
    /**
     * Module runs only on Paper (Minecraft server) platform.
     */
    PAPER,
    
    /**
     * Module runs only on Velocity (proxy) platform.
     */
    VELOCITY,
    
    /**
     * Module runs on both Paper and Velocity platforms.
     * The module implementation should handle platform-specific logic.
     */
    BOTH
}
