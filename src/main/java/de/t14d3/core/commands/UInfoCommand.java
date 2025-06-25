package de.t14d3.core.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.OfflinePlayerArgument;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UInfoCommand {
    public UInfoCommand() {
        Main main = Main.getInstance();
        new CommandAPICommand("uinfo")
                .withArguments(new OfflinePlayerArgument("player"))
                .withPermission("core.uinfo")
                .executes((sender, args) -> {
                    OfflinePlayer target = (OfflinePlayer) args.get("player");
                    if(target == null) {
                        sender.sendMessage(main.getMessage("&cPlayer not found"));
                        return;
                    }

                    sender.sendMessage(main.getMessage("&6Player Info: &e" + target.getName()));
                    sender.sendMessage(main.getMessage("&7UUID: &f" + target.getUniqueId()));

                    if (target.hasPlayedBefore()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String firstPlayed = sdf.format(new Date(target.getFirstPlayed()));
                        String lastPlayed = sdf.format(new Date(target.getLastSeen()));

                        sender.sendMessage(main.getMessage("&7First Played: &f" + firstPlayed));
                        sender.sendMessage(main.getMessage("&7Last Seen: &f" + lastPlayed));

                        sender.sendMessage(main.getMessage("&7Location: &f" + target.getLocation().getBlockX() + ", " +
                                target.getLocation().getBlockY() + ", " + target.getLocation().getBlockZ()));
                    }

                    if (target.isOnline() && target instanceof Player player) {
                        sender.sendMessage(main.getMessage("&7GameMode: &f" + player.getGameMode()));
                        sender.sendMessage(main.getMessage("&7Health: &f" + player.getHealth() + "/" + player.getMaxHealth()));
                        sender.sendMessage(main.getMessage("&7Food: &f" + player.getFoodLevel() + "/" + player.getFoodLevel()));
                        sender.sendMessage(main.getMessage("&7Saturation: &f" + player.getSaturation() + "/" + player.getSaturation()));
                        sender.sendMessage(main.getMessage("&7XP: &f" + player.getExp() + "/" + player.getExp()));
                        sender.sendMessage(main.getMessage("&7Level: &f" + player.getLevel()));
                        sender.sendMessage(main.getMessage("&7World: &f" + player.getWorld().getName()));
                    }

                })
                .withFullDescription("Shows information about a player.");
    }
}
