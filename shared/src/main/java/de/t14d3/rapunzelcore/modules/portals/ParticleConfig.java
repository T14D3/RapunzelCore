package de.t14d3.rapunzelcore.modules.portals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Configuration for particle effects displayed by portals.
 */
public record ParticleConfig(
    @NotNull String particleType,
    int count,
    double offsetX, double offsetY, double offsetZ,
    double speed,
    boolean enabled,
    @Nullable String color, // For colored particles (hex format: #RRGGBB)
    int intervalTicks // How often particles spawn (in ticks)
) {
    public ParticleConfig {
        Objects.requireNonNull(particleType, "particleType cannot be null");
        if (count < 0) count = 0;
        if (intervalTicks < 1) intervalTicks = 1;
    }

    /**
     * Creates a default particle configuration for portals.
     *
     * @return a default particle config
     */
    public static ParticleConfig defaults() {
        return new ParticleConfig("PORTAL", 5, 0.5, 0.5, 0.5, 0.1, true, null, 5);
    }

    /**
     * Creates a particle configuration with the specified type.
     *
     * @param particleType the particle type name
     * @return a new particle config
     */
    public static ParticleConfig of(@NotNull String particleType) {
        return new ParticleConfig(particleType, 5, 0.5, 0.5, 0.5, 0.1, true, null, 5);
    }

    /**
     * Creates a colored particle configuration.
     *
     * @param particleType the particle type name
     * @param color hex color code (e.g., "#FF0000")
     * @return a new particle config
     */
    public static ParticleConfig colored(@NotNull String particleType, @NotNull String color) {
        return new ParticleConfig(particleType, 5, 0.5, 0.5, 0.5, 0.1, true, color, 5);
    }

    /**
     * Creates a builder for fluent configuration.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String particleType = "PORTAL";
        private int count = 5;
        private double offsetX = 0.5, offsetY = 0.5, offsetZ = 0.5;
        private double speed = 0.1;
        private boolean enabled = true;
        private String color;
        private int intervalTicks = 5;

        public Builder particleType(String particleType) {
            this.particleType = particleType;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder offset(double x, double y, double z) {
            this.offsetX = x;
            this.offsetY = y;
            this.offsetZ = z;
            return this;
        }

        public Builder speed(double speed) {
            this.speed = speed;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder interval(int intervalTicks) {
            this.intervalTicks = intervalTicks;
            return this;
        }

        public ParticleConfig build() {
            return new ParticleConfig(particleType, count, offsetX, offsetY, offsetZ,
                speed, enabled, color, intervalTicks);
        }
    }
}
