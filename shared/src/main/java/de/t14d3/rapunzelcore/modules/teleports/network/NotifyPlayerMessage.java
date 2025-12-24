package de.t14d3.rapunzelcore.modules.teleports.network;

public record NotifyPlayerMessage(
    String playerUuid,
    String messageKey,
    String[] args
) {
    public static NotifyPlayerMessage of(String playerUuid, String messageKey, String... args) {
        return new NotifyPlayerMessage(playerUuid, messageKey, args == null ? new String[0] : args);
    }
}
