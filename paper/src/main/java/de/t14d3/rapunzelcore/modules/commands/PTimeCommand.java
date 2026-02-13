package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PTimeCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("ptime")
                .withAliases("playertime")
                .withFullDescription("Sets personal time for the player.")
                .withPermission("rapunzelcore.commands.ptime")
                .withOptionalArguments(
                        new StringArgument("time")
                                .replaceSuggestions((sender, builder) -> {
                                    builder.suggest("day");
                                    builder.suggest("night");
                                    builder.suggest("noon");
                                    builder.suggest("midnight");
                                    builder.suggest("reset");
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.commands.ptime.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    String timeArg = args.get("time") == null ? "day" : (String) args.get("time");
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    long timeValue;
                    String timeName;
                    
                    switch (timeArg.toLowerCase()) {
                        case "day":
                            timeValue = 1000L;
                            timeName = "day";
                            break;
                        case "night":
                            timeValue = 13000L;
                            timeName = "night";
                            break;
                        case "noon":
                            timeValue = 6000L;
                            timeName = "noon";
                            break;
                        case "midnight":
                            timeValue = 18000L;
                            timeName = "midnight";
                            break;
                        case "reset":
                            target.resetPlayerTime();
                            if (sender.equals(target)) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.reset.self"));
                            } else {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.reset.other", target.getName()));
                                target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.reset.target", sender.getName()));
                            }
                            return Command.SINGLE_SUCCESS;
                        default:
                            // Try to parse as number
                            try {
                                timeValue = Long.parseLong(timeArg);
                                timeName = String.valueOf(timeValue);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.error.invalid", timeArg));
                                return Command.SINGLE_SUCCESS;
                            }
                    }

                    target.setPlayerTime(timeValue, false);
                    
                    if (sender.equals(target)) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.success.self", timeName));
                    } else {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.success.other", target.getName(), timeName));
                        target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.ptime.success.target", sender.getName(), timeName));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
