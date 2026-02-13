package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.config.YamlConfig;

/**
 * Moderation module for managing player moderation actions.
 * Provides ban, kick, mute, jail, and warn functionality.
 */
public class ModerationModule implements Module {
    private boolean enabled = false;
    private RapunzelCore core;
    private ModerationModuleImpl moderationImpl;
    private YamlConfig config;

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        this.core = core;
        this.config = loadConfig();

        // Create platform-specific implementation
        moderationImpl = core.getPlatformManager().createModerationModuleImpl(core, config);
        if (moderationImpl != null) {
            moderationImpl.initialize();
        }

        RapunzelCore.getLogger().info("Moderation module enabled.");
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (moderationImpl != null) {
            moderationImpl.cleanup();
        }
        moderationImpl = null;
        core = null;
        config = null;
    }

    @Override
    public String getName() {
        return "moderation";
    }

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public RapunzelCore getCore() {
        return core;
    }

    /**
     * Gets the module configuration.
     * @return The configuration
     */
    public YamlConfig getConfig() {
        return config;
    }

    /**
     * Interface for platform-specific moderation module implementations.
     */
    public interface ModerationModuleImpl {
        /** Initialize the platform-specific implementation. */
        void initialize();

        /** Clean up resources. */
        void cleanup();
    }
}
