package de.t14d3.rapunzelcore.modules.teleports;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelVelocityCore;
import de.t14d3.rapunzelcore.modules.teleports.network.ProxyConnectRequest;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzellib.network.NetworkEventBus;

import java.util.Optional;
import java.util.UUID;

public class TeleportsProxyModule implements Module, AutoCloseable {
 private boolean enabled;
 private RapunzelCore core;

 private NetworkEventBus bus;
 private NetworkEventBus.Subscription connectSub;

 @Override
 public Environment getEnvironment() {
 return Environment.VELOCITY;
 }

 @Override
 public void enable(RapunzelCore core) {
 if (enabled) return;
 this.core = core;
 enabled = true;

 RapunzelVelocityCore main = (RapunzelVelocityCore) core;
 ProxyServer proxy = main.getServer();

 this.bus = new NetworkEventBus(main.getMessenger());

 this.connectSub = bus.register(NetworkChannels.TELEPORTS_PROXY, ProxyConnectRequest.class, (payload, sourceServer) -> {
 if (payload == null || payload.playerUuid() == null || payload.targetServer() == null) return;
 UUID playerId;
 try {
 playerId = UUID.fromString(payload.playerUuid());
 } catch (Exception ignored) {
 return;
 }

 Optional<Player> playerOpt = proxy.getPlayer(playerId);
 if (playerOpt.isEmpty()) return;

 Optional<RegisteredServer> target = proxy.getServer(payload.targetServer());
 if (target.isEmpty()) return;

 playerOpt.get().createConnectionRequest(target.get()).connect();
 });
 }

 @Override
 public void disable() {
 if (!enabled) return;
 enabled = false;
 close();
 }

 @Override
 public String getName() {
 return "teleports";
 }

 @Override
 public boolean isEnabled() {
 return enabled;
 }

 @Override
 public RapunzelCore getCore() {
 return core;
 }

 @Override
 public void close() {
 if (connectSub != null) connectSub.close();
 connectSub = null;
 bus = null;
 }
}
