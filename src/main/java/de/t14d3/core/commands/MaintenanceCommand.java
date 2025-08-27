package de.t14d3.core.commands;


import com.mojang.brigadier.Command;
import de.t14d3.core.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;


public class MaintenanceCommand {


    public MaintenanceCommand() {
        new CommandAPICommand("maintenance")
                .withFullDescription("Toggle maintenance mode. Players without bypass permission will be prevented from joining.")
                .withPermission("core.maintenance")
                .withArguments(new StringArgument("state").replaceSuggestions((sender, builder) -> {
                    builder.suggest("on");
                    builder.suggest("off");
                    builder.suggest("toggle");
                    builder.suggest("status");
                    return builder.buildFuture();
                }))
                .executes((executor, args) -> {
                    String state = (String) args.get("state");
                    if (state == null) {
                        executor.sendMessage(Main.getInstance().getMessage("commands.maintenance.error.invalid", "null"));
                        return Command.SINGLE_SUCCESS;
                    }


                    state = state.toLowerCase();
                    boolean newState;
                    switch (state) {
                        case "on":
                            newState = true;
                            break;
                        case "off":
                            newState = false;
                            break;
                        case "toggle":
                            newState = !Main.getInstance().isMaintenanceMode();
                            break;
                        case "status":
                            executor.sendMessage(Main.getInstance().getMessage("commands.maintenance.status", String.valueOf(Main.getInstance().isMaintenanceMode())));
                            return Command.SINGLE_SUCCESS;
                        default:
                            executor.sendMessage(Main.getInstance().getMessage("commands.maintenance.error.invalid", state));
                            return Command.SINGLE_SUCCESS;
                    }


                    Main.getInstance().setMaintenanceMode(newState);


                    if (newState) {
                        Bukkit.broadcast(Main.getInstance().getMessage("commands.maintenance.enabled"));
                    } else {
                        Bukkit.broadcast(Main.getInstance().getMessage("commands.maintenance.disabled"));
                    }


                    return Command.SINGLE_SUCCESS;
                })
                .register(Main.getInstance());
    }
}