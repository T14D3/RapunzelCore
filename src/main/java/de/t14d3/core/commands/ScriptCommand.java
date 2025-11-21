package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import de.t14d3.core.ScriptManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;

public class ScriptCommand {
    public ScriptCommand(ScriptManager scriptManager) {
        new CommandAPICommand("script")
                .withArguments(new GreedyStringArgument("script"))
                .executes((executor, args) -> {
                    String script = (String) args.get("script");
                    scriptManager.executeScript(executor, script);
                    return Command.SINGLE_SUCCESS;
                })
                .withPermission("core.script")
                .register(Main.getInstance());
    }
}
