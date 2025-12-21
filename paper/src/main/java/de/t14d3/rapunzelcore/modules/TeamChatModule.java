package de.t14d3.rapunzelcore.modules;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TeamChatModule implements Module {
    private boolean enabled = false;
    private RapunzelPaperCore plugin;

    @Override
    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        if (environment != Environment.PAPER) return;
        this.plugin = (RapunzelPaperCore) core;
        enabled = true;

        registerCommands();
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
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

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.teamchat.use", "op"),
                Map.entry("rapunzelcore.teamchat", "op"),
                Map.entry("rapunzelcore.teamchat.see", "op")
        );
    }

    private void registerCommands() {
        new CommandAPICommand("teamchat")
                .withAliases("tc")
                .withPermission("rapunzelcore.teamchat.use")
                .withArguments(new StringArgument("message")
                        .withPermission("rapunzelcore.teamchat")
                )
                .executes((executor, args) -> {
                    Bukkit.getScheduler().runTaskAsynchronously((Plugin) RapunzelCore.getInstance(), () -> {
                        Player sender = (Player) executor;
                        String raw = (String) args.get("message");
                        Set<Player> recipients = Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("rapunzelcore.teamchat.see"))
                                .collect(Collectors.toSet());
                        Component message = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.teamchat.format.sender",
                                sender.getName(), raw);
                        recipients.forEach(player -> {
                            player.sendMessage(message);
                        });
                    });

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sends a message to team chat.")
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
