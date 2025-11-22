package de.t14d3.core.modules;

import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.stream.Collectors;

public class TeamChatModule implements Module {
    private boolean enabled = false;
    private Main plugin;

    @Override
    public void enable(Main plugin) {
        if (enabled) return;
        this.plugin = plugin;
        enabled = true;

        registerCommands();
    }

    @Override
    public void disable(Main plugin) {
        if (!enabled) return;
        // CommandAPI commands are automatically unregistered on plugin disable, but we can clean up here if needed
        enabled = false;
    }

    @Override
    public String getName() {
        return "teamchat";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void registerCommands() {
        new CommandAPICommand("teamchat")
                .withAliases("tc")
                .withPermission("core.teamchat.use")
                .withArguments(new StringArgument("message")
                        .withPermission("core.teamchat")
                )
                .executes((executor, args) -> {
                    Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                        Player sender = (Player) executor;
                        String raw = (String) args.get("message");
                        Set<Player> recipients = Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("core.teamchat.see"))
                                .collect(Collectors.toSet());
                        Component message = Main.getInstance().getMessage("commands.teamchat.format.sender",
                                sender.getName(), raw);
                        recipients.forEach(player -> {
                            player.sendMessage(message);
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sends a message to team chat.")
                .register(Main.getInstance());
    }
}
