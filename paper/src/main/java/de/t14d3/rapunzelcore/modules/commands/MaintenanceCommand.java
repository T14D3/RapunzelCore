package de.t14d3.rapunzelcore.modules.commands;


import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class MaintenanceCommand implements Listener, Command {
    private static boolean maintenanceMode;

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, (Plugin) RapunzelCore.getInstance());
        new CommandAPICommand("maintenance")
                .withFullDescription("Toggle maintenance mode. Players without bypass permission will be prevented from joining.")
                .withPermission("rapunzelcore.maintenance")
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
                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.maintenance.error.invalid", "null"));
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
                            newState = !maintenanceMode;
                            break;
                        case "status":
                            executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.maintenance.status", String.valueOf(maintenanceMode)));
                            return Command.SINGLE_SUCCESS;
                        default:
                            executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.maintenance.error.invalid", state));
                            return Command.SINGLE_SUCCESS;
                    }

                    maintenanceMode = newState;

                    Bukkit.broadcast(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.maintenance." + (newState ? "enabled" : "disabled")));

                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!maintenanceMode) return;
        if (event.getPlayer().hasPermission("rapunzelcore.maintenance.bypass")) return;
        Component kickMessage = RapunzelCore.getInstance().getMessageHandler().getMessage("commands.maintenance.kick");
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);
    }

    @Override
    public void unregister() {
        CommandAPI.unregister("maintenance");
        PlayerLoginEvent.getHandlerList().unregister(this);
    }
}