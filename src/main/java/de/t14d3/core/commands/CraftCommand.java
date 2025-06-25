package de.t14d3.core.commands;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

public class CraftCommand {
    public CraftCommand() {
        new CommandAPICommand("craft")
                .withFullDescription("Opens a crafting inventory for the player.")
                .withPermission("core.craft")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    MenuType.Typed.CRAFTING.builder().build(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}
