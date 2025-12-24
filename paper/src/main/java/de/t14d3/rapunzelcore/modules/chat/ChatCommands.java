package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Channel;
import de.t14d3.rapunzelcore.database.entities.PlayerEntity;
import de.t14d3.rapunzelcore.database.entities.PlayerRepository;
import de.t14d3.rapunzelcore.util.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatCommands {
    private final RapunzelPaperCore plugin;
    private final ChannelManager channelManager;
    private final PaperChannelBroadcaster broadcaster;

    public ChatCommands(RapunzelPaperCore plugin, ChannelManager channelManager, PaperChannelBroadcaster broadcaster) {
        this.plugin = plugin;
        this.channelManager = channelManager;
        this.broadcaster = broadcaster;
    }

    public void register() {
        // Msg command
        new CommandAPICommand("msg")
                .withAliases("tell", "whisper", "w")
                .withArguments(
                        new EntitySelectorArgument.OnePlayer("player"),
                        new GreedyStringArgument("message")
                )
                .withPermission("rapunzelcore.msg")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    Player target = (Player) args.get("player");
                    String message = (String) args.get("message");

                    if (target == null) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.msg.error.invalid", args.getRaw("player")));
                        return 1;
                    }

                    Component senderMessage = plugin.getMessageHandler().getMessage("commands.msg.format.sender",
                                    target.getName(), message)
                            .color(NamedTextColor.GRAY);
                    Component targetMessage = plugin.getMessageHandler().getMessage("commands.msg.format.target",
                                    sender.getName(), message)
                            .color(NamedTextColor.GRAY);

                    sender.sendMessage(senderMessage);
                    target.sendMessage(targetMessage);

                    return 1;
                })
                .withFullDescription("Sends a private message to the given player.")
                .register(plugin);

        // Broadcast command
        new CommandAPICommand("broadcast")
                .withAliases("bc")
                .withArguments(new GreedyStringArgument("message"))
                .withFullDescription("Broadcasts a message to all online players.")
                .withPermission("rapunzelcore.broadcast")
                .executes((executor, args) -> {
                    String message = (String) args.get("message");
                    Component broadcastMessage = plugin.getMessageHandler().getMessage("commands.broadcast.format",
                            message, executor.getName());
                    Bukkit.broadcast(broadcastMessage);
                    return 1;
                })
                .register(plugin);

        // Socialspy
        new CommandAPICommand("socialspy")
                .withAliases("spy")
                .withFullDescription("Toggles social spy mode for the given player.")
                .withPermission("rapunzelcore.socialspy")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    boolean enabled = senderEntity.isSocialSpyEnabled();
                    senderEntity.setSocialSpyEnabled(!enabled);
                    CoreDatabase.runLocked(() -> PlayerRepository.getInstance().save(senderEntity));
                    CoreDatabase.flushAsync();
                    Component message = plugin.getMessageHandler().getMessage("commands.socialspy.toggle", !enabled ? "enabled" : "disabled");
                    sender.sendMessage(message);
                    return 1;
                })
                .register(plugin);

        // Root "channel" command (will hold subcommands)
        CommandAPICommand channelRoot = new CommandAPICommand("channel")
                .withAliases("ch")
                .withPermission("rapunzelcore.channel")
                .withFullDescription("Manages chat channels.");

        // /channel list
        CommandAPICommand listSub = new CommandAPICommand("list")
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    StringBuilder channelsList = new StringBuilder();
                    for (Channel channel : channelManager.getAllowedChannels(senderEntity).values()) {
                        channelsList.append(channel.getName()).append(", ");
                    }
                    if (channelsList.length() > 0) {
                        channelsList.setLength(channelsList.length() - 2); // Remove trailing ", "
                    }
                    Component message = plugin.getMessageHandler().getMessage("commands.channel.list", channelsList.toString());
                    sender.sendMessage(message);
                    return 1;
                });

        // /channel join <channel>
        CommandAPICommand joinSub = new CommandAPICommand("join")
                .withArguments(
                        new StringArgument("channel")
                                .replaceSuggestions((info, builder) -> {
                                    if (info.sender() instanceof Player sender) {
                                        PlayerEntity senderEntity = Utils.player(sender);
                                        channelManager.getAllowedChannels(senderEntity).forEach((name, channel) -> builder.suggest(name));
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    String channelArg = (String) args.get("channel");
                    if (channelArg == null || channelArg.isEmpty()) {
                        sender.sendMessage(Component.text("Usage: /channel join <name>").color(NamedTextColor.RED));
                        return 1;
                    }
                    Channel channel = channelManager.getChannel(channelArg);
                    if (channel == null) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.notfound", channelArg));
                        return 1;
                    }
                    if (!channel.hasPermission(senderEntity)) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.nopermission", channel.getName()));
                        return 1;
                    }
                    if (channelManager.joinChannel(senderEntity, channel)) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.joined", channel.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.join", channel.getName()));
                    }
                    return 1;
                });

        // /channel leave <channel>
        CommandAPICommand leaveSub = new CommandAPICommand("leave")
                .withArguments(
                        new StringArgument("channel")
                                .replaceSuggestions((info, builder) -> {
                                    if (info.sender() instanceof Player sender) {
                                        PlayerEntity senderEntity = Utils.player(sender);
                                        channelManager.getJoinedChannels(senderEntity).stream()
                                            .map(Channel::getName)
                                            .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    String channelArg = (String) args.get("channel");
                    if (channelArg == null || channelArg.isEmpty()) {
                        sender.sendMessage(Component.text("Usage: /channel leave <name>").color(NamedTextColor.RED));
                        return 1;
                    }
                    Channel channel = channelManager.getChannel(channelArg);
                    if (channel == null) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.notfound", channelArg));
                        return 1;
                    }
                    if (channelManager.leaveChannel(senderEntity, channel)) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.left", channel.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.leave", channel.getName()));
                    }
                    return 1;
                });

        // /channel main <channel>
        CommandAPICommand mainSub = new CommandAPICommand("main")
                .withArguments(
                        new StringArgument("channel")
                                .replaceSuggestions((info, builder) -> {
                                    if (info.sender() instanceof Player sender) {
                                        PlayerEntity senderEntity = Utils.player(sender);
                                        channelManager.getJoinedChannels(senderEntity).stream()
                                            .map(Channel::getName)
                                            .forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                )
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    String channelArg = (String) args.get("channel");
                    if (channelArg == null || channelArg.isEmpty()) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.main", ""));
                        return 1;
                    }
                    Channel channel = channelManager.getChannel(channelArg);
                    if (channel == null) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.notfound", channelArg));
                        return 1;
                    }
                    if (channelManager.setMainChannel(senderEntity, channel)) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.main", channel.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.main", channel.getName()));
                    }
                    return 1;
                });

        // /channel send <channel> <message...>
        CommandAPICommand sendSub = new CommandAPICommand("send")
                .withArguments(
                        new StringArgument("channel")
                                .replaceSuggestions((info, builder) -> {
                                    if (info.sender() instanceof Player sender) {
                                        PlayerEntity senderEntity = Utils.player(sender);
                                        channelManager.getAllowedChannels(senderEntity).keySet().forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                }),
                        new GreedyStringArgument("message")
                )
                .executes((executor, args) -> {
                    if (!(executor instanceof Player sender)) return 1;
                    PlayerEntity senderEntity = Utils.player(sender);
                    String channelArg = (String) args.get("channel");
                    String messageArg = (String) args.get("message");
                    if (channelArg == null || channelArg.isEmpty() || messageArg == null || messageArg.isEmpty()) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.send", ""));
                        return 1;
                    }
                    Channel channel = channelManager.getChannel(channelArg);
                    if (channel == null) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.notfound", channelArg));
                        return 1;
                    }
                    if (!channel.hasPermission(senderEntity)) {
                        sender.sendMessage(plugin.getMessageHandler().getMessage("commands.channel.error.nopermission", channel.getName()));
                        return 1;
                    }
                    if (!channelManager.isJoined(senderEntity, channel)) {
                        channelManager.joinChannel(senderEntity, channel);
                    }
                    broadcaster.broadcastOutgoing(sender, channel, Component.text(messageArg));
                    return 1;
                });

        // Add subcommands to root
        channelRoot
                .withSubcommand(listSub)
                .withSubcommand(joinSub)
                .withSubcommand(leaveSub)
                .withSubcommand(mainSub)
                .withSubcommand(sendSub);

        // Register the root (which registers all subcommands)
        channelRoot.register(plugin);
    }

    public void unregister() {
        // Unregister commands
        dev.jorel.commandapi.CommandAPI.unregister("msg");
        dev.jorel.commandapi.CommandAPI.unregister("broadcast");
        dev.jorel.commandapi.CommandAPI.unregister("socialspy");
        dev.jorel.commandapi.CommandAPI.unregister("channel");
    }
}
