package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("craft")
                .withFullDescription("Opens a crafting inventory for the player.")
                .withPermission("rapunzelcore.craft")
                .executes((executor, args) -> {
                    Player player = (Player) executor;
                    // noinspection UnstableApiUsage
                    MenuType.Typed.CRAFTING.builder().build(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
