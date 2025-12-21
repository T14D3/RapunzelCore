package de.t14d3.rapunzelcore.messaging;

import com.google.gson.Gson;
import de.t14d3.rapunzelcore.RapunzelCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper-side implementation of {@link Messenger} using Minecraft plugin messaging.
 *
 * <p>Messages are sent to Velocity via an arbitrary online player connection.
 * If there are no players online on this backend, messages are dropped (which is acceptable per requirements).</p>
 */
public class PaperPluginMessenger implements Messenger, PluginMessageListener {

    public static final String TRANSPORT_CHANNEL = "rapunzelcore:bridge";

    private final JavaPlugin plugin;
    private final Gson gson = new Gson();

    private final Map<String, List<MessageListener>> listeners = new ConcurrentHashMap<>();

    public PaperPluginMessenger(JavaPlugin plugin) {
        this.plugin = plugin;

        // Register incoming/outgoing transport channel.
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, TRANSPORT_CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, TRANSPORT_CHANNEL);
    }

    @Override
    public void sendToAll(String channel, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(String channel, String data) {
        sendEnvelope(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
    }

    private void sendEnvelope(NetworkEnvelope env) {
        Player carrier = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            // No one online -> no connection to proxy; drop.
            return;
        }

        byte[] bytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        carrier.sendPluginMessage(plugin, TRANSPORT_CHANNEL, bytes);
    }

    @Override
    public void registerListener(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(String channel, MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null) return;
        list.remove(listener);
    }

    @Override
    public boolean isConnected() {
        // We can only send when there is an online player.
        return !Bukkit.getOnlinePlayers().isEmpty();
    }

    @Override
    public String getServerName() {
        return Bukkit.getServer().getName();
    }

    @Override
    public String getProxyServerName() {
        return "velocity";
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!TRANSPORT_CHANNEL.equals(channel)) return;

        String json = new String(message, StandardCharsets.UTF_8);
        NetworkEnvelope env;
        try {
            env = gson.fromJson(json, NetworkEnvelope.class);
        } catch (Exception e) {
            RapunzelCore.getLogger().warn("Failed to parse network envelope: {}", e.getMessage());
            return;
        }

        if (env == null || env.getChannel() == null) return;

        // Deliver to listeners of the logical channel.
        List<MessageListener> list = listeners.get(env.getChannel());
        if (list == null || list.isEmpty()) return;

        for (MessageListener listener : List.copyOf(list)) {
            try {
                listener.onMessage(env.getChannel(), env.getData(), env.getSourceServer());
            } catch (Exception e) {
                RapunzelCore.getLogger().warn("Network listener error on channel {}: {}", env.getChannel(), e.getMessage());
            }
        }
    }
}
