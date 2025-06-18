package de.bydennyy.byDennyysEssentials;

import de.bydennyy.byDennyysEssentials.alias.SimpleAlias;
import de.bydennyy.byDennyysEssentials.command.LightningCommand;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;


@SuppressWarnings({"UnstableApiUsage", "unused"})
public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {

            commands.registrar().register(SimpleAlias.createCommand("gm0", "gamemode survival", "minecraft.command.gamemode.survival"));
            commands.registrar().register(SimpleAlias.createCommand("gm1", "gamemode creative", "minecraft.command.gamemode.creative"));
            commands.registrar().register(SimpleAlias.createCommand("gm2", "gamemode adventure", "minecraft.command.gamemode.adventure"));
            commands.registrar().register(SimpleAlias.createCommand("gm3", "gamemode spectator", "minecraft.command.gamemode.spectator"));


            commands.registrar().register(SimpleAlias.createCommand("tpall", "tp @a ~ ~ ~", "minecraft.command.teleport"));

            commands.registrar().register(LightningCommand.createCommand());
        });
    }

}
