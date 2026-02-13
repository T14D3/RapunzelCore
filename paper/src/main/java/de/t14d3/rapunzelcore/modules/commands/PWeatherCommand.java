package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PWeatherCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("pweather")
                .withAliases("playerweather")
                .withFullDescription("Sets personal weather for the player.")
                .withPermission("rapunzelcore.commands.pweather")
                .withOptionalArguments(
                        new StringArgument("weather")
                                .replaceSuggestions((sender, builder) -> {
                                    builder.suggest("clear");
                                    builder.suggest("rain");
                                    builder.suggest("thunder");
                                    builder.suggest("reset");
                                    return builder.buildFuture();
                                })
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.commands.pweather.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    String weatherArg = args.get("weather") == null ? "clear" : (String) args.get("weather");
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    switch (weatherArg.toLowerCase()) {
                        case "clear":
                        case "sun":
                            target.setPlayerWeather(WeatherType.CLEAR);
                            if (sender.equals(target)) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.self", "clear"));
                            } else {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.other", target.getName(), "clear"));
                                target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.target", sender.getName(), "clear"));
                            }
                            break;
                        case "rain":
                        case "downfall":
                            target.setPlayerWeather(WeatherType.DOWNFALL);
                            if (sender.equals(target)) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.self", "rain"));
                            } else {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.other", target.getName(), "rain"));
                                target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.target", sender.getName(), "rain"));
                            }
                            break;
                        case "thunder":
                        case "storm":
                            // Note: Bukkit API doesn't have a separate thunder weather type for players
                            // Thunder is just rain with thunder, so we set to downfall
                            target.setPlayerWeather(WeatherType.DOWNFALL);
                            if (sender.equals(target)) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.self", "thunder"));
                            } else {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.other", target.getName(), "thunder"));
                                target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.success.target", sender.getName(), "thunder"));
                            }
                            break;
                        case "reset":
                            target.resetPlayerWeather();
                            if (sender.equals(target)) {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.reset.self"));
                            } else {
                                sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.reset.other", target.getName()));
                                target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.reset.target", sender.getName()));
                            }
                            break;
                        default:
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.pweather.error.invalid", weatherArg));
                            return Command.SINGLE_SUCCESS;
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
