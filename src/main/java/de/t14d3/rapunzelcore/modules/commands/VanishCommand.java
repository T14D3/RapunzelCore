package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanishCommand implements Listener, Command {
    private static final Map<Player, Boolean> vanishedPlayers = new HashMap<>();

    @Override
    public void register() {
        new CommandAPICommand("vanish")
                .withAliases("v")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("rapunzelcore.vanish")
                        .replaceSuggestions((sender, builder) -> {
                            Bukkit.getOnlinePlayers().forEach(player -> builder.suggest(player.getName()));
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Toggles vanish mode for the given player.")
                .withPermission("rapunzelcore.vanish")
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    // Determine target: if argument provided, use it; otherwise use sender
                    Player target = args.get("player") != null ? (Player) args.get("player") : sender;

                    if (target == null) {
                        sender.sendMessage(
                                Main.getInstance().getMessageHandler().getMessage("commands.vanish.error.invalid", args.getRaw("player"))
                        );
                        return Command.SINGLE_SUCCESS;
                    }

                    boolean isVanished = vanishedPlayers.getOrDefault(target, false);
                    vanishedPlayers.put(target, !isVanished);

                    if (!isVanished) {
                        // Enable vanish: hide from players without see permission
                        for (Player viewer : Bukkit.getOnlinePlayers()) {
                            if (!viewer.hasPermission("rapunzelcore.vanish.see") && !viewer.equals(target)) {
                                viewer.hidePlayer(Main.getInstance(), target);
                            } else {
                                viewer.sendMessage(Main.getInstance().getMessageHandler().getMessage("commands.vanish.fakemessage.leave.see", target.getName()));
                            }
                        }
                        Bukkit.getServer().broadcast(Main.getInstance().getMessageHandler().getMessage("commands.vanish.fakemessage.leave", target.getName()));
                    } else {
                        // Disable vanish: show to everyone
                        for (Player viewer : Bukkit.getOnlinePlayers()) {
                            if (!viewer.equals(target)) {
                                viewer.showPlayer(Main.getInstance(), target);
                            } else {
                                viewer.sendMessage(Main.getInstance().getMessageHandler().getMessage("commands.vanish.fakemessage.join.see", target.getName()));
                            }
                        }
                        Bukkit.getServer().broadcast(Main.getInstance().getMessageHandler().getMessage("commands.vanish.fakemessage.join", target.getName()));
                    }

                    // Notify sender
                    Component message = Main.getInstance()
                            .getMessageHandler()
                            .getMessage("commands.vanish.toggle", target.getName(), !isVanished ? "enabled" : "disabled")
                            .color(!isVanished ? NamedTextColor.GREEN : NamedTextColor.RED);
                    sender.sendMessage(message);
                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @Override
    public void unregister() {
        vanishedPlayers.clear();
        PlayerJoinEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiner = event.getPlayer();
        boolean canSee = joiner.hasPermission("rapunzelcore.vanish.see");
        if (canSee) {
            // Show list of vanished players to joiner
            List<String> vanishedPlayersList = new ArrayList<>();
            for (Map.Entry<Player, Boolean> entry : vanishedPlayers.entrySet()) {
                Player vanished = entry.getKey();
                boolean isVanished = entry.getValue();
                if (isVanished) {
                    vanishedPlayersList.add(vanished.getName());
                }
            }
            Component message = Main.getInstance().getMessageHandler().getMessage("commands.vanish.list", String.join(", ", vanishedPlayersList));
            joiner.sendMessage(message);
        }
        // Hide existing vanished players from joiner if joiner lacks see permission
        for (Map.Entry<Player, Boolean> entry : vanishedPlayers.entrySet()) {
            Player vanished = entry.getKey();
            boolean isVanished = entry.getValue();

            if (isVanished && !canSee && !joiner.equals(vanished)) {
                joiner.hidePlayer(Main.getInstance(), vanished);
            }
        }
    }
}
