package de.t14d3.core;

import de.t14d3.core.commands.*;

public class CommandManager {
    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        new BroadcastCommand();
        new EnderChestCommand();
        new FlyCommand();
        new FlySpeedCommand();
        new InvSeeCommand();
        new MsgCommand();
        new OfflineTpCommand();
        new SocialSpyCommand();
        new SpeedCommand();
        new TeamChatCommand();
        new UInfoCommand();
        new VanishCommand();

    }
}
