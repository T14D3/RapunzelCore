package de.t14d3.rapunzelcore.modules.teleports.network;

public record ProxyConnectRequest(
    String playerUuid,
    String targetServer
) {
}

