package de.t14d3.core.modules.commands;

import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;


import java.util.concurrent.atomic.AtomicInteger;

public class RestartCommand implements Command {
    private static BukkitTask restartTask;
    private static final AtomicInteger remainingSeconds = new AtomicInteger(0);

    @Override
    public void register() {
        Main plugin = Main.getInstance();


        new CommandAPICommand("restart")
                .withFullDescription("Schedules or cancels a server restart.")
                .withPermission("core.restart")
                .withArguments(new StringArgument("secondsOrCancel")
                        .replaceSuggestions((sender, builder) -> {
                            builder.suggest("cancel");
                            builder.suggest("10");
                            builder.suggest("30");
                            builder.suggest("60");
                            builder.suggest("300");
                            return builder.buildFuture();
                        }))
                .executes((executor, args) -> {
                    String raw = (String) args.get("secondsOrCancel");
                    if (raw == null) {
                        executor.sendMessage(Main.getInstance().getMessage("commands.restart.error.invalid", "null"));
                        return Command.SINGLE_SUCCESS;
                    }


                    if (raw.equalsIgnoreCase("cancel")) {
                        if (restartTask != null) {
                            restartTask.cancel();
                            restartTask = null;
                            remainingSeconds.set(0);
                            Component message = Main.getInstance().getMessage("commands.restart.cancelled");
                            Bukkit.broadcast(message);
                        } else {
                            executor.sendMessage(Main.getInstance().getMessage("commands.restart.error.none"));
                        }
                        return Command.SINGLE_SUCCESS;
                    }


                    int seconds;
                    try {
                        seconds = Integer.parseInt(raw);
                        if (seconds <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException ex) {
                        executor.sendMessage(Main.getInstance().getMessage("commands.restart.error.invalid", raw));
                        return Command.SINGLE_SUCCESS;
                    }


                    if (restartTask != null) {
                        restartTask.cancel();
                        restartTask = null;
                    }


                    remainingSeconds.set(seconds);


                    Bukkit.broadcast(Main.getInstance().getMessage("commands.restart.started", String.valueOf(seconds)));


                    restartTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        int left = remainingSeconds.decrementAndGet();


                        if (left > 0) {
                            // Broadcast selected ticks: every minute, every 10s under a minute, and each second under 5s
                            if (left % 60 == 0 || left <= 10 || (left <= 60 && left % 10 == 0)) {
                                Bukkit.broadcast(Main.getInstance().getMessage("commands.restart.tick", String.valueOf(left)));
                            }
                        } else {
                            Bukkit.broadcast(Main.getInstance().getMessage("commands.restart.complete"));
                            restartTask.cancel();
                            restartTask = null;
                            remainingSeconds.set(0);
                            Bukkit.shutdown();
                        }


                    }, 20L, 20L);


                    return Command.SINGLE_SUCCESS;
                })
                .register(plugin);
    }


    public static boolean isRestartScheduled() {
        return restartTask != null;
    }


    public static int getRemainingSeconds() {
        return remainingSeconds.get();
    }
}