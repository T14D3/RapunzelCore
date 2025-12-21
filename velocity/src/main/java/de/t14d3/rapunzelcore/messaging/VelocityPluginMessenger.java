package de.t14d3.rapunzelcore.messaging;

import com.google.gson.Gson;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.t14d3.rapunzelcore.RapunzelCore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Velocity-side implementation of {@link Messenger}.
 *
 * <p>This is the authoritative router: Paper backends send plugin messages to Velocity, and Velocity
 * forwards to target backend servers by piggy-backing on any player that is currently connected to that backend.</p>
 */
public class VelocityPluginMessenger implements Messenger {

    public static final String TRANSPORT_CHANNEL = "rapunzelcore:bridge";
    public static final ChannelIdentifier CHANNEL_ID = MinecraftChannelIdentifier.from(TRANSPORT_CHANNEL);

    private final Object plugin;
    private final ProxyServer proxy;
    private final Gson gson = new Gson();

    private final Map<String, List<MessageListener>> listeners = new ConcurrentHashMap<>();

    public VelocityPluginMessenger(Object plugin, ProxyServer proxy) {
        this.plugin = plugin;
        this.proxy = proxy;

        proxy.getChannelRegistrar().register(CHANNEL_ID);
        proxy.getEventManager().register(plugin, this);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL_ID)) return;

        // Determine direction.
        // - If source is a ServerConnection: message came from backend -> route and (optionally) deliver to proxy listeners.
        // - If source is a Player: message came from player client (shouldn't happen for our channel).
        Object source = event.getSource();
        if (!(source instanceof ServerConnection serverConn)) {
            return;
        }

        String originServer = serverConn.getServerInfo().getName();
        String json = new String(event.getData(), StandardCharsets.UTF_8);

        NetworkEnvelope env;
        try {
            env = gson.fromJson(json, NetworkEnvelope.class);
        } catch (Exception e) {
            RapunzelCore.getLogger().warn("Failed to parse network envelope from backend {}: {}", originServer, e.getMessage());
            return;
        }
        if (env == null || env.getChannel() == null) return;

        // Normalize/override origin.
        env.setSourceServer(originServer);

        // First, allow proxy-side listeners for this logical channel.
        deliverToLocalListeners(env);

        // Then route to backends if requested.
        switch (env.getTarget()) {
            case PROXY -> {
                // do nothing
            }
            case ALL -> forwardToAllBackends(env);
            case SERVER -> {
                if (env.getTargetServer() != null && !env.getTargetServer().isBlank()) {
                    forwardToBackend(env.getTargetServer(), env);
                }
            }
            default -> {
                // ignore
            }
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    private void forwardToAllBackends(NetworkEnvelope env) {
        for (String serverName : proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList()) {
            // Don’t echo back to origin server.
            if (serverName.equalsIgnoreCase(env.getSourceServer())) continue;
            forwardToBackend(serverName, env);
        }
    }

    private void forwardToBackend(String serverName, NetworkEnvelope env) {
        Optional<com.velocitypowered.api.proxy.server.RegisteredServer> rsOpt = proxy.getServer(serverName);
        if (rsOpt.isEmpty()) return;

        // We can only send plugin messages to a backend if at least one player is connected to it.
        Optional<Player> carrier = proxy.getAllPlayers().stream()
            .filter(p -> p.getCurrentServer().map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(serverName)).orElse(false))
            .findFirst();

        if (carrier.isEmpty()) {
            return;
        }

        Optional<ServerConnection> connection = carrier.get().getCurrentServer();
        if (connection.isEmpty()) return;

        byte[] bytes = gson.toJson(env).getBytes(StandardCharsets.UTF_8);
        connection.get().sendPluginMessage(CHANNEL_ID, bytes);
    }

    private void deliverToLocalListeners(NetworkEnvelope env) {
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

    @Override
    public void sendToAll(String channel, String data) {
        // Velocity -> all backends.
        NetworkEnvelope env = new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis());
        forwardToAllBackends(env);
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        NetworkEnvelope env = new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis());
        forwardToBackend(serverName, env);
    }

    @Override
    public void sendToProxy(String channel, String data) {
        // Already on proxy.
        NetworkEnvelope env = new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis());
        deliverToLocalListeners(env);
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
        // Proxy is always connected to itself.
        return true;
    }

    @Override
    public String getServerName() {
        return getProxyServerName();
    }

    @Override
    public String getProxyServerName() {
        return "velocity";
    }
}
