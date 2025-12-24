package de.t14d3.rapunzelcore.commands;

import com.mojang.brigadier.Command;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.configsync.CoreConfigSync;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.filesync.FileSyncRole;
import dev.jorel.commandapi.CommandAPICommand;

public final class ConfigSyncCommand {
    private ConfigSyncCommand() {
    }

    public static CommandAPICommand command() {
        return new CommandAPICommand("configsync")
            .withFullDescription("Sync config files across servers.")
            .withPermission("rapunzelcore.admin")
            .withSubcommands(
                new CommandAPICommand("status")
                    .executes((executor, args) -> {
                        RapunzelPaperCore plugin = (RapunzelPaperCore) RapunzelCore.getInstance();
                        if (!CoreConfigSync.isEnabled()) {
                            executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.disabled"));
                            return Command.SINGLE_SUCCESS;
                        }
                        FileSyncRole role = CoreConfigSync.role();
                        executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.status", String.valueOf(role)));
                        return Command.SINGLE_SUCCESS;
                    }),
                new CommandAPICommand("broadcast")
                    .executes((executor, args) -> {
                        RapunzelPaperCore plugin = (RapunzelPaperCore) RapunzelCore.getInstance();
                        if (!CoreConfigSync.isEnabled()) {
                            executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.disabled"));
                            return Command.SINGLE_SUCCESS;
                        }
                        CoreConfigSync.broadcastInvalidate();
                        executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.broadcasted"));
                        return Command.SINGLE_SUCCESS;
                    }),
                new CommandAPICommand("sync")
                    .executes((executor, args) -> {
                        RapunzelPaperCore plugin = (RapunzelPaperCore) RapunzelCore.getInstance();
                        if (!CoreConfigSync.isEnabled()) {
                            executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.disabled"));
                            return Command.SINGLE_SUCCESS;
                        }

                        FileSyncRole role = CoreConfigSync.role();
                        if (role == FileSyncRole.AUTHORITY) {
                            CoreConfigSync.broadcastInvalidate();
                            executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.broadcasted"));
                            return Command.SINGLE_SUCCESS;
                        }

                        executor.sendMessage(plugin.getMessageHandler().getMessage("commands.configsync.sync.started"));
                        CoreConfigSync.requestSync().whenComplete((result, error) -> {
                            Rapunzel.context().scheduler().run(() -> {
                                if (error != null) {
                                    executor.sendMessage(plugin.getMessageHandler().getMessage(
                                        "commands.configsync.sync.error",
                                        String.valueOf(error.getMessage())
                                    ));
                                    return;
                                }

                                executor.sendMessage(plugin.getMessageHandler().getMessage(
                                    "commands.configsync.sync.success",
                                    String.valueOf(result.filesWritten()),
                                    String.valueOf(result.filesDeleted())
                                ));
                            });
                        });

                        return Command.SINGLE_SUCCESS;
                    })
            );
    }
}
