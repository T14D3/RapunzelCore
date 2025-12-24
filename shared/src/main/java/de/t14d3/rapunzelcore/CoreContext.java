package de.t14d3.rapunzelcore;

/**
 * Global holder for the active RapunzelCore instance.
 *
 * <p>Needed because Java interface static methods cannot be overridden per-platform.</p>
 */
public final class CoreContext {
    private static volatile RapunzelCore instance;

    private CoreContext() {}

    public static RapunzelCore getInstance() {
        RapunzelCore current = instance;
        if (current == null) {
            throw new IllegalStateException("RapunzelCore instance not initialized yet");
        }
        return current;
    }

    public static void setInstance(RapunzelCore core) {
        if (core == null) {
            throw new IllegalArgumentException("core cannot be null");
        }
        instance = core;
    }
}

