package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.listeners.ChatListener;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class SocialSpyCommand implements Command {
    private ChatListener listener;

    @Override
    public void register() {
        listener = new ChatListener(Main.getInstance());
        Main.getInstance().getServer().getPluginManager().registerEvents(listener, Main.getInstance());

        new CommandAPICommand("socialspy")
                .withAliases("spy")
                .withFullDescription("Toggles social spy mode for the given player.")
                .withPermission("rapunzelcore.socialspy")
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    boolean enabled = sender.hasMetadata("socialspy");
                    if (enabled) {
                        sender.removeMetadata("socialspy", Main.getInstance());
                    } else {
                        sender.setMetadata("socialspy", new FixedMetadataValue(Main.getInstance(), true));
                    }
                    Component message = Main.getInstance().getMessage("commands.socialspy.toggle",!enabled ? "enabled" : "disabled");
                    sender.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("socialspy");
        listener.unregister();
    }
}
