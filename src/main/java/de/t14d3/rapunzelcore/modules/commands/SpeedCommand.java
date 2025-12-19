package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.Main;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SpeedCommand implements Command {
    private static final float DEFAULT_SPEED = 0.2F;
    private static final float MAX_SPEED = 1.0F;

    @Override
    public void register() {
        new CommandAPICommand("speed")
                .withArguments(
                        // allow user to specify 1–10, where 1 → default speed (0.2), 10 → max speed (1.0)
                        new FloatArgument("speed", 0.0F, 10.0F)
                )
                .withOptionalArguments(
                        new EntitySelectorArgument.OnePlayer("player")
                                .withPermission("rapunzelcore.speed")
                                .replaceSuggestions((sender, builder) -> {
                                    Bukkit.getOnlinePlayers().forEach(p -> builder.suggest(p.getName()));
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    Player sender = (Player) executor;
                    Player target = args.get("player") == null
                            ? sender
                            : (Player) args.get("player");

                    float input = (float) args.get("speed");
                    // normalize input from [1,10] to [0,1]
                    float normalized = (input - 1.0F) / 9.0F;
                    float mappedSpeed = DEFAULT_SPEED + normalized * (MAX_SPEED - DEFAULT_SPEED);

                    target.setWalkSpeed(mappedSpeed);

                    Component message = Main.getInstance().getMessageHandler().getMessage(
                            "commands.speed.set",
                            target.getName(),
                            String.format("%.2f", mappedSpeed)
                    );
                    sender.sendMessage(message);

                    return Command.SINGLE_SUCCESS;
                })
                .withFullDescription("Sets movement speed between default (0.2) and 1.0 using values 1–10.")
                .register(Main.getInstance());
    }
}
