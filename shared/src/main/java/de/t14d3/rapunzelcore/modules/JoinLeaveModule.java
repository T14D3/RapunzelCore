package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.config.YamlConfig;

import java.nio.file.Path;

/**
 * Join/leave module entrypoint that delegates to platform-specific implementations.
 */
public class JoinLeaveModule implements Module {
    private boolean enabled;
    private JoinLeaveModuleImpl joinLeaveImpl;

    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        enabled = true;

        Path configPath = getConfigPath();
        YamlConfig config = loadConfig();
        boolean networkEnabled = config.getBoolean("network.enabled", true);
        config.save();

        joinLeaveImpl = core.getPlatformManager().createJoinLeaveModuleImpl(core, networkEnabled, configPath);
        if (joinLeaveImpl != null) {
            joinLeaveImpl.initialize();
        }
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        enabled = false;

        if (joinLeaveImpl != null) {
            joinLeaveImpl.cleanup();
            joinLeaveImpl = null;
        }
    }

    @Override
    public String getName() {
        return "joinleave";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public interface JoinLeaveModuleImpl {
        void initialize();
        void cleanup();
    }

    /**
     * Minimal signal used to let backends know the proxy will handle join/leave broadcasts.
     */
    public record JoinLeavePayload(boolean proxyHandlesBroadcasts) {
    }
}
