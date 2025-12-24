package de.t14d3.rapunzelcore.modules.chat;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;

/**
 * Paper-specific implementation of the ChatModule.
 * Handles Bukkit/Spigot/Paper-specific chat functionality.
 */
public class PaperChatModuleImpl implements ChatModule.ChatModuleImpl {
    private final RapunzelCore core;
    private final ChannelManager channelManager;

    private ChatCommands chatCommands;
    private ChatListener chatListener;
    private PaperChannelBroadcaster broadcaster;

    public PaperChatModuleImpl(RapunzelCore core, ChannelManager channelManager) {
        this.core = core;
        this.channelManager = channelManager;
    }

    @Override
    public void initialize() {
        RapunzelPaperCore paperCore = (RapunzelPaperCore) core;

        broadcaster = new PaperChannelBroadcaster(paperCore, channelManager);
        broadcaster.startIncomingListener();
        
        // Register Paper-specific commands
        chatCommands = new ChatCommands(paperCore, channelManager, broadcaster);
        chatCommands.register();

        // Register Paper-specific event listeners
        chatListener = new ChatListener(paperCore, channelManager, broadcaster);
        
        RapunzelCore.getLogger().info("Paper ChatModule implementation initialized");
    }

    @Override
    public void cleanup() {
        if (chatCommands != null) {
            chatCommands.unregister();
        }
        if (chatListener != null) {
            chatListener.unregister();
        }
        if (broadcaster != null) {
            broadcaster.stopIncomingListener();
        }
    }
}
