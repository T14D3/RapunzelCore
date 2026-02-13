package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerTimeCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("ptime")
                .withFullDescription("Sets your personal time.")
                .withPermission("rapunzelcore.playertime")
                .withOptionalArguments(
                        new IntegerArgument("time", 0, 24000)
                                .replaceSuggestions((sender, builder) -> {
                                    builder.suggest(0);
                                    builder.suggest(6000);
                                    builder.suggest(12000);
                                    builder.suggest(18000);
                                    builder.suggest(24000);
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.playertime.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Integer time = args.get("time") == null ? null : (Integer) args.get("time");
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (time == null) {
                        // Reset to server time
                        target.resetPlayerTime();
                        if (sender.equals(target)) {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.reset.self"));
                        } else {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.reset.other", target.getName()));
                            target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.reset.target", sender.getName()));
                        }
                    } else {
                        target.setPlayerTime(time, false);
                        String timeName = getTimeName(time);
                        if (sender.equals(target)) {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.set.self", timeName));
                        } else {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.set.other", target.getName(), timeName));
                            target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playertime.set.target", sender.getName(), timeName));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    private String getTimeName(int time) {
        if (time < 1000) return "sunrise";
        if (time < 7000) return "morning";
        if (time < 11000) return "noon";
        if (time < 13000) return "afternoon";
        if (time < 17000) return "sunset";
        if (time < 19000) return "evening";
        if (time < 23000) return "midnight";
        return "night";
    }
}
