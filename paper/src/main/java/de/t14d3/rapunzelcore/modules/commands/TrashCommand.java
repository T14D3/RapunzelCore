package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class TrashCommand implements Command, Listener {

    private static final String TRASH_INVENTORY_TITLE = "<red>Trash - Items will be deleted!";

    @Override
    public void register() {
        new CommandAPICommand("trash")
                .withAliases("disposal", "garbage", "bin")
                .withFullDescription("Opens a trash inventory that deletes items when closed.")
                .withPermission("rapunzelcore.commands.trash")
                .executesPlayer((player, args) -> {
                    openTrashInventory(player);
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
        
        // Register the listener
        Bukkit.getPluginManager().registerEvents(this, (JavaPlugin) RapunzelCore.getInstance());
    }

    private void openTrashInventory(Player player) {
        Inventory trashInventory = Bukkit.createInventory(player, 54, RapunzelCore.getInstance().getMessageHandler().getMessage("commands.trash.title"));
        player.openInventory(trashInventory);
        player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.trash.opened"));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Check if this is a trash inventory
        if (event.getView().title().equals(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.trash.title"))) {
            Player player = (Player) event.getPlayer();
            // Items in the inventory are automatically dropped/deleted when inventory closes
            // We just notify the player that items were deleted
            if (!event.getInventory().isEmpty()) {
                int itemCount = 0;
                for (int i = 0; i < event.getInventory().getSize(); i++) {
                    if (event.getInventory().getItem(i) != null) {
                        itemCount++;
                    }
                }
                player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.trash.deleted", String.valueOf(itemCount)));
            }
        }
    }
}
