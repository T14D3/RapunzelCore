package de.t14d3.rapunzelcore.modules.script;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Main;
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
                .withPermission("rapunzelcore.script")
                .register(Main.getInstance());
    }
}
