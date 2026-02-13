package de.t14d3.rapunzelcore.modules.moderation;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Paper-specific implementation of the ModerationModule.
 * Handles Bukkit/CommandAPI registration for moderation commands.
 */
public class PaperModerationModuleImpl implements ModerationModule.ModerationModuleImpl {
    private final RapunzelCore core;
    private final YamlConfig config;
    private final SpoolDatabase database;
    private MuteManager muteManager;
    private ModerationListener moderationListener;
    private WarnCommand warnCommand;
    private BanCommand banCommand;
    private KickCommand kickCommand;
    private MuteCommand muteCommand;
    private JailCommand jailCommand;
    private final List<ModerationCommand> commands = new ArrayList<>();

    public PaperModerationModuleImpl(RapunzelCore core, YamlConfig config) {
        this.core = core;
        this.config = config;
        this.database = core.getCoreDatabase();
    }

    @Override
    public void initialize() {
        // Initialize MuteManager
        this.muteManager = new MuteManager(core, database);

        // Register listener for chat integration
        this.moderationListener = new ModerationListener(muteManager);
        ((JavaPlugin) core).getServer().getPluginManager().registerEvents(moderationListener, (JavaPlugin) core);

        // Initialize and register all moderation commands
        this.banCommand = new BanCommand(muteManager);
        this.banCommand.register();
        commands.add(banCommand);

        this.kickCommand = new KickCommand(muteManager);
        this.kickCommand.register();
        commands.add(kickCommand);

        this.muteCommand = new MuteCommand(muteManager);
        this.muteCommand.register();
        commands.add(muteCommand);

        this.jailCommand = new JailCommand(muteManager);
        this.jailCommand.register();
        commands.add(jailCommand);

        // Initialize WarnCommand with plugin and database
        this.warnCommand = new WarnCommand(core, database);
        this.warnCommand.register();
        commands.add(warnCommand);

        RapunzelCore.getLogger().info("Paper moderation module initialized with ban, kick, mute, jail, warn commands.");
    }

    @Override
    public void cleanup() {
        // Unregister all commands
        for (ModerationCommand command : commands) {
            command.unregister();
        }
        commands.clear();

        // Cleanup MuteManager
        if (muteManager != null) {
            muteManager.cleanup();
        }

        // Unregister listener
        if (moderationListener != null) {
            moderationListener.unregister();
        }

        muteManager = null;
        moderationListener = null;
        warnCommand = null;
        banCommand = null;
        kickCommand = null;
        muteCommand = null;
        jailCommand = null;
    }

    /**
     * Gets the MuteManager instance.
     *
     * @return The mute manager
     */
    public MuteManager getMuteManager() {
        return muteManager;
    }

    /**
     * Gets the WarnCommand instance.
     *
     * @return The warn command handler
     */
    public WarnCommand getWarnCommand() {
        return warnCommand;
    }
}
