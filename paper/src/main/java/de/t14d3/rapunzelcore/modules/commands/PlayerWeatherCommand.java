package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerWeatherCommand implements Command {

    @Override
    public void register() {
        new CommandAPICommand("pweather")
                .withFullDescription("Sets your personal weather.")
                .withPermission("rapunzelcore.playerweather")
                .withOptionalArguments(
                        new MultiLiteralArgument("weather", "clear", "downfall", "reset")
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.playerweather.others")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    String weather = args.get("weather") == null ? null : (String) args.get("weather");
                    Player target = args.get("player") == null ? sender : (Player) args.get("player");

                    if (target == null) {
                        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("general.error.player.invalid", args.getRaw("player")));
                        return Command.SINGLE_SUCCESS;
                    }

                    if (weather == null || weather.equals("reset")) {
                        // Reset to server weather
                        target.resetPlayerWeather();
                        if (sender.equals(target)) {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.reset.self"));
                        } else {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.reset.other", target.getName()));
                            target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.reset.target", sender.getName()));
                        }
                    } else {
                        WeatherType weatherType = weather.equals("clear") ? WeatherType.CLEAR : WeatherType.DOWNFALL;
                        target.setPlayerWeather(weatherType);
                        if (sender.equals(target)) {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.set.self", weather));
                        } else {
                            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.set.other", target.getName(), weather));
                            target.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.playerweather.set.target", sender.getName(), weather));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }
}
