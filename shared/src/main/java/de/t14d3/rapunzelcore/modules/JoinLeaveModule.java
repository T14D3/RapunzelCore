package de.t14d3.rapunzelcore.modules;

import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.Rapunzel;

import java.nio.file.Path;

/**
 * Join/leave module entrypoint that delegates to platform-specific implementations.
 */
public class JoinLeaveModule implements Module {
    private JoinLeaveModuleImpl joinLeaveImpl;
    private YamlConfig config;
    private boolean enabled = false;

    @Override
    public void enable(RapunzelCore core) {
        this.enabled = true;
        this.config = loadConfig();
        Path configPath = getConfigPath();
        if (config != null) {
            config.save();
        }

        joinLeaveImpl = core.getPlatformManager().createJoinLeaveModuleImpl(core, config, configPath);
        if (joinLeaveImpl != null) {
            joinLeaveImpl.initialize();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
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
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public YamlConfig loadConfig() {
        String configFileName = "joinleave.yml";
        Path dataDir = Rapunzel.context().dataDirectory();
        Path configPath = dataDir.resolve("modules").resolve(configFileName);
        return Rapunzel.context().configs().load(configPath, configFileName);
    }

    public Path getConfigPath() {
        String configFileName = "joinleave.yml";
        Path dataDir = Rapunzel.context().dataDirectory();
        return dataDir.resolve("modules").resolve(configFileName);
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
