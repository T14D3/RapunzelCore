package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlaytimeCommand implements Command {

    public void register() {
        Main plugin = Main.getInstance();
        new CommandAPICommand("playtime")
                .withOptionalArguments(new EntitySelectorArgument.OnePlayer("player")
                        .withPermission("rapunzelcore.playtime.others")
                        .replaceSuggestions((sender, builder) -> {
                            plugin.getServer().getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                            return builder.buildFuture();
                        })
                )
                .withFullDescription("Shows the formatted playtime of the target player.")
                .withPermission("rapunzelcore.playtime")
                .executes((executor, args) -> {
                    Player target = args.get("player") == null ? (Player) executor : (Player) args.get("player");
                    int ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
                    long seconds = ticks / 20L;
                    String formattedTime = formatPlaytime(seconds);
                    executor.sendMessage(plugin.getMessageHandler().getMessage("commands.playtime.success", target.getName(), formattedTime));
                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }

    private String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
