package de.t14d3.core.modules.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class AnvilCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("anvil")
                .withFullDescription("Opens an anvil inventory for the player.")
                .withPermission("core.anvil")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    MenuType.Typed.ANVIL.builder().build(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}

