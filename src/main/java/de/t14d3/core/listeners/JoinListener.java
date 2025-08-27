package de.t14d3.core.listeners;

import de.t14d3.core.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class JoinListener implements Listener {


    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!Main.getInstance().isMaintenanceMode()) return;
        if (event.getPlayer().hasPermission("core.maintenance.bypass")) return;
        Component kickMessage = Main.getInstance().getMessage("commands.maintenance.kick");
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(kickMessage));
    }
}