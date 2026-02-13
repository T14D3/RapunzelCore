package de.t14d3.rapunzelcore.modules.itemframe;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.modules.commands.Command;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Main /itemframe command with subcommands.
 */
public class ItemFrameCommand implements Command {

    private final RapunzelPaperCore plugin;
    private final ItemFrameModule module;

    // Subcommands
    private final ItemFrameInvisibleCommand invisibleCommand;
    private final ItemFrameLockCommand lockCommand;
    private final ItemFrameUnlockCommand unlockCommand;

    public ItemFrameCommand(RapunzelPaperCore plugin, ItemFrameModule module) {
        this.plugin = plugin;
        this.module = module;
        
        // Initialize subcommands
        this.invisibleCommand = new ItemFrameInvisibleCommand(plugin, module);
        this.lockCommand = new ItemFrameLockCommand(plugin, module);
        this.unlockCommand = new ItemFrameUnlockCommand(plugin, module);
        
        register();
    }

    @Override
    public void register() {
        // Main /itemframe command - shows help
        new CommandAPICommand("itemframe")
                .withAliases("if")
                .withFullDescription("Item frame management commands")
                .withPermission("rapunzelcore.itemframe")
                .executesPlayer((player, args) -> {
                    showHelp(player);
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);

        // /itemframe invisible - Toggle visibility
        new CommandAPICommand("itemframe")
                .withSubcommand(new CommandAPICommand("invisible")
                        .withAliases("invis", "i")
                        .withFullDescription("Toggle item frame visibility")
                        .withPermission("rapunzelcore.itemframe.invisible")
                        .executesPlayer((player, args) -> {
                            invisibleCommand.toggle(player);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(plugin);

        // /itemframe lock - Lock item frame
        new CommandAPICommand("itemframe")
                .withSubcommand(new CommandAPICommand("lock")
                        .withAliases("l")
                        .withFullDescription("Lock an item frame")
                        .withPermission("rapunzelcore.itemframe.lock")
                        .executesPlayer((player, args) -> {
                            lockCommand.lock(player);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(plugin);

        // /itemframe unlock - Unlock item frame
        new CommandAPICommand("itemframe")
                .withSubcommand(new CommandAPICommand("unlock")
                        .withAliases("ul")
                        .withFullDescription("Unlock an item frame")
                        .withPermission("rapunzelcore.itemframe.unlock")
                        .executesPlayer((player, args) -> {
                            unlockCommand.unlock(player);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(plugin);
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.help.header"));
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.help.invisible"));
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.help.lock"));
        player.sendMessage(plugin.getMessageHandler().getMessage("itemframe.help.unlock"));
    }

    @Override
    public void unregister() {
        List.of("itemframe", "if").forEach(CommandAPI::unregister);
    }
}
