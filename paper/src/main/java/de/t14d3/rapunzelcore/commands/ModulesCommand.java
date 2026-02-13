package de.t14d3.rapunzelcore.commands;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.ModuleManager;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.ModuleDescriptor;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ModulesCommand {

    public static CommandAPICommand command() {
        return new CommandAPICommand("modules")
                .withFullDescription("Manage modules.")
                .withPermission("rapunzelcore.modules")
                .withArguments(new StringArgument("action")
                        .replaceSuggestions((info, builder) -> {
                            builder
                                    .suggest("enable")
                                    .suggest("disable")
                                    .suggest("reload")
                                    .suggest("list");

                            return builder.buildFuture();
                        })
                )
                .withOptionalArguments(
                new StringArgument("module")
                        .replaceSuggestions((info, builder) -> {
                            String action = (String) info.previousArgs().get("action");
                            if (action == null) {
                                return builder.buildFuture();
                            }
                            
                            RapunzelPaperCore core = RapunzelPaperCore.getInstance();
                            if (core == null) {
                                return builder.buildFuture();
                            }
                            ModuleManager moduleManager = core.getModuleManager();
                            
                            return switch (action.toLowerCase()) {
                                case "enable" -> {
                                    moduleManager.getModules().forEach(module -> {
                                        if (!module.isEnabled()) {
                                            builder.suggest(module.name());
                                        }
                                    });
                                    yield builder.buildFuture();
                                }
                                case "disable", "reload" -> {
                                    moduleManager.getModules().forEach(module -> {
                                        if (module.isEnabled()) {
                                            builder.suggest(module.name());
                                        }
                                    });
                                    yield builder.buildFuture();
                                }
                                default -> builder.buildFuture();
                            };
                        })
                )
                .executes((executor, args) -> {
                    String action = (String) args.get("action");
                    String moduleName = (String) args.get("module");
                    if (action == null) {
                        executor.sendMessage(Component.text("Invalid action"));
                        return Command.SINGLE_SUCCESS;
                    }

                    RapunzelPaperCore core = RapunzelPaperCore.getInstance();
                    if (core == null) {
                        executor.sendMessage(Component.text("Core not initialized."));
                        return Command.SINGLE_SUCCESS;
                    }
                    ModuleManager moduleManager = core.getModuleManager();

                    switch (action.toLowerCase()) {
                        case "list" -> {
                            Component message = Component.empty();
                            for (ModuleDescriptor module : moduleManager.getModules()) {
                                message = message.append(Component.text("\n - " + module.name()).color(module.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
                            }
                            executor.sendMessage(message);
                            return Command.SINGLE_SUCCESS;
                        }
                        case "disable" -> {
                            if (moduleManager.disable(moduleName)) {
                                executor.sendMessage(Component.text("Module disabled."));
                            } else {
                                executor.sendMessage(Component.text("Module not found."));
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                        case "reload" -> {
                            if (moduleManager.reload(moduleName)) {
                                executor.sendMessage(Component.text("Module reloaded."));
                            } else {
                                executor.sendMessage(Component.text("Module not found."));
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                        case "enable" -> {
                            if (moduleManager.enable(moduleName)) {
                                executor.sendMessage(Component.text("Module enabled."));
                            } else {
                                executor.sendMessage(Component.text("Module not found."));
                            }
                            return Command.SINGLE_SUCCESS;
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }
}
