package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.info.NetworkInfoService;

import java.util.concurrent.CompletableFuture;

/**
 * Small helper to resolve network/server information for teleports without
 * maintaining a separate runtime cache.
 */
final class TeleportsNetwork {
    private TeleportsNetwork() {
    }

    static String localServerNameIfKnown() {
        if (!Rapunzel.isBootstrapped()) return null;
        try {
            Messenger messenger = Rapunzel.context().services().get(Messenger.class);
            if (messenger == null) return null;
            String name = messenger.getServerName();
            if (name == null) return null;
            String trimmed = name.trim();
            if (trimmed.isBlank() || "unknown".equalsIgnoreCase(trimmed)) return null;
            return trimmed;
        } catch (Exception ignored) {
            return null;
        }
    }

    static CompletableFuture<String> resolveLocalServerName() {
        if (!Rapunzel.isBootstrapped()) return CompletableFuture.completedFuture(null);
        try {
            NetworkInfoService info = Rapunzel.context().services().get(NetworkInfoService.class);
            return info.networkServerName().exceptionally(ignored -> null);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    static boolean isLocal(String serverName) {
        if (serverName == null || serverName.isBlank()) return true;
        String local = localServerNameIfKnown();
        if (local == null) return false;
        return local.equalsIgnoreCase(serverName.trim());
    }

    static final class TeleportsActions {
        static final String HOME = "HOME";
        static final String WARP = "WARP";
        static final String TPA_TO_PLAYER = "TPA_TO_PLAYER";

        private TeleportsActions() {
        }
    }
}

