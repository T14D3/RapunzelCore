package de.t14d3.core.modules.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UInfoCommand implements Command {

    @Override
    public void register() {
        Main main = Main.getInstance();
        var argument = new AsyncPlayerProfileArgument("player");
        new CommandAPICommand("uinfo")
                .withArguments(argument)
                .withPermission("core.uinfo")
                .executes((sender, args) -> {
                    CompletableFuture<List<PlayerProfile>> future = args.getByArgument(argument);
                    future.thenAccept(profiles -> {
                        PlayerProfile profile = profiles.getFirst();
                        if (profile == null || profile.getId() == null) {
                            sender.sendMessage(main.getMessage("commands.uinfo.error.invalid", args.getRaw("player")));
                            return;
                        }
                        OfflinePlayer target = Bukkit.getOfflinePlayer(profile.getId());

                        List<Component> lines = new ArrayList<>();

                        lines.add(main.getMessage("commands.uinfo.player", target.getName()));
                        lines.add(main.getMessage("commands.uinfo.uuid", target.getUniqueId().toString()));

                        if (target.hasPlayedBefore()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String firstPlayed = sdf.format(new Date(target.getFirstPlayed()));
                            String lastPlayed  = sdf.format(new Date(target.getLastSeen()));

                            lines.add(main.getMessage("commands.uinfo.firstplayed", firstPlayed));
                            lines.add(main.getMessage("commands.uinfo.lastseen", lastPlayed));
                            lines.add(main.getMessage(
                                    "commands.uinfo.location",
                                    String.valueOf(target.getLocation().getBlockX()),
                                    String.valueOf(target.getLocation().getBlockY()),
                                    String.valueOf(target.getLocation().getBlockZ())
                            ));
                        }

                        if (target.isOnline() && target instanceof Player player) {
                            lines.add(main.getMessage("commands.uinfo.gamemode", player.getGameMode().toString()));
                            lines.add(main.getMessage("commands.uinfo.health", String.valueOf(player.getHealth()), String.valueOf(player.getMaxHealth())));
                            lines.add(main.getMessage("commands.uinfo.food", String.valueOf(player.getFoodLevel())));
                            lines.add(main.getMessage("commands.uinfo.saturation", String.valueOf(player.getSaturation())));
                            lines.add(main.getMessage("commands.uinfo.level", String.valueOf(player.getLevel())));
                            lines.add(main.getMessage("commands.uinfo.world", player.getWorld().getName()));
                        }

                        Component message = Component.join(
                                JoinConfiguration.separator(Component.newline()),
                                lines
                        );

                        sender.sendMessage(message);

                    }).exceptionally(e -> {
                        sender.sendMessage(e.getMessage());
                        return null;
                    });
                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Shows information about a player.")
                .register(Main.getInstance());
    }
}
