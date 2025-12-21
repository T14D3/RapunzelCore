package de.t14d3.rapunzelcore.modules.script;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class AliasCommand {
    private final ScriptManager scriptManager;

    public AliasCommand(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;

        new CommandAPICommand("alias")
                .withSubcommand(new CommandAPICommand("add")
                        .withArguments(new StringArgument("name"), new GreedyStringArgument("script"))
                        .executes((executor, args) -> {
                            String name = (String) args.get("name");
                            String script = (String) args.get("script");
                            scriptManager.addAlias(name, script, executor);
                            return Command.SINGLE_SUCCESS;
                        }))
                .withSubcommand(new CommandAPICommand("remove")
                        .withArguments(new StringArgument("name"))
                        .executes((executor, args) -> {
                            String name = (String) args.get("name");
                            scriptManager.removeAlias(name);
                            executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.removed", name));
                            return Command.SINGLE_SUCCESS;
                        }))
                .withSubcommand(new CommandAPICommand("list")
                        .executes((executor, args) -> {
                            var aliases = scriptManager.getAliases();
                            if (aliases.isEmpty()) {
                                executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.list.empty"));
                            } else {
                                executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.list.header"));
                                aliases.forEach((name, data) -> {
                                    if (data.permission == null || !(executor instanceof Player player) || player.hasPermission(data.permission)) {
                                        Component editButton = Component.text("[Edit]").color(NamedTextColor.GREEN)
                                                .clickEvent(ClickEvent.runCommand("/alias edit " + name));
                                        executor.sendMessage(Component.text("- ").append(Component.text(name)).append(Component.text(" ")).append(editButton));
                                    }
                                });
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .withSubcommand(new CommandAPICommand("edit")
                        .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.stringCollection(this::getVisibleAliases)))
                        .executes((executor, args) -> {
                            String name = (String) args.get("name");
                            showEditor(executor, name);
                            return Command.SINGLE_SUCCESS;
                        })
                        .withSubcommand(new CommandAPICommand("set")
                                .withArguments(new IntegerArgument("index"), new GreedyStringArgument("content"))
                                .executes((executor, args) -> {
                                    String name = (String) args.get("name");
                                    int index = (Integer) args.get("index");
                                    String content = (String) args.get("content");
                                    scriptManager.editAliasLine(name, index - 1, content, executor);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .withSubcommand(new CommandAPICommand("add")
                                .withArguments(new GreedyStringArgument("content"))
                                .executes((executor, args) -> {
                                    String name = (String) args.get("name");
                                    String content = (String) args.get("content");
                                    scriptManager.addAliasLine(name, content, executor);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .withSubcommand(new CommandAPICommand("remove")
                                .withArguments(new IntegerArgument("index"))
                                .executes((executor, args) -> {
                                    String name = (String) args.get("name");
                                    int index = (Integer) args.get("index");
                                    scriptManager.removeAliasLine(name, index - 1);
                                    executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.line.removed"));
                                    showEditor(executor, name);
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .withSubcommand(new CommandAPICommand("permission")
                                .withArguments(new GreedyStringArgument("permission").setOptional(true))
                                .executes((executor, args) -> {
                                    String name = (String) args.get("name");
                                    String perm = (String) args.getOrDefault("permission", "");
                                    var aliases = scriptManager.getAliases();
                                    ScriptManager.AliasData data = aliases.get(name);
                                    if (data != null) {
                                        if (perm.isEmpty() || "clear".equals(perm)) {
                                            data.permission = null;
                                            executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.permission.cleared"));
                                        } else {
                                            data.permission = perm;
                                            executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.permission.set", perm));
                                        }
                                        scriptManager.saveAliases();
                                    } else {
                                        executor.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.not.found"));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .withPermission("rapunzelcore.alias")
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    private void showEditor(CommandSender sender, String name) {
        var aliases = scriptManager.getAliases();
        ScriptManager.AliasData data = aliases.get(name);
        if (data == null) {
            sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.not.found"));
            return;
        }
        String script = data.script;
        String[] lines = script.split("\n");
        sender.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("alias.editor.header", name));
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Component lineComp = Component.text((i + 1) + ": " + line);
            Component editButton = Component.text(" [Edit]").color(NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.suggestCommand("/alias edit " + name + " set " + (i + 1) + " " + line));
            Component removeButton = Component.text(" [Remove]").color(NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/alias edit " + name + " remove " + (i + 1)));
            sender.sendMessage(lineComp.append(editButton).append(removeButton));
        }
        Component addButton = Component.text("[Add Line]").color(NamedTextColor.GREEN)
                .clickEvent(ClickEvent.suggestCommand("/alias edit " + name + " add "));
        sender.sendMessage(addButton);
        Component permButton = Component.text("[Set Permission]").color(NamedTextColor.BLUE)
                .clickEvent(ClickEvent.suggestCommand("/alias edit " + name + " permission "));
        sender.sendMessage(permButton);
    }

    private List<String> getVisibleAliases(SuggestionInfo<CommandSender> info) {
        CommandSender sender = info.sender();
        var aliases = scriptManager.getAliases();
        return aliases.entrySet().stream()
                .filter(entry -> entry.getValue().permission == null ||
                        !(sender instanceof Player player) ||
                        player.hasPermission(entry.getValue().permission))
                .map(Map.Entry::getKey)
                .toList();
    }
}
