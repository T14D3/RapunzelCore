package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class AnvilCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("anvil")
                .withFullDescription("Opens an anvil inventory for the player.")
                .withPermission("rapunzelcore.anvil")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    // noinspection UnstableApiUsage
                    MenuType.Typed.ANVIL.builder().build(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}

