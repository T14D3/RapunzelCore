package de.t14d3.core;

import de.t14d3.core.commands.*;

public class CommandManager {
    private final Main plugin;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        new AnvilCommand();
        new BackCommand();
        new BroadcastCommand();
        new CraftCommand();
        new DelHomeCommand(plugin);
        new EnderChestCommand();
        new FlyCommand();
        new FlySpeedCommand();
        new HomeCommand(plugin);
        new InvSeeCommand();
        new MsgCommand();
        new OfflineTpCommand();
        new SetHomeCommand(plugin);
        new SocialSpyCommand();
        new SpawnCommand();
        new SpeedCommand();
        new TeamChatCommand();
        new UInfoCommand();
        new VanishCommand();

    }
}
