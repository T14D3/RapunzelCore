package de.t14d3.rapunzelcore.services.entitytransfer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.network.NetworkChannels;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzelcore.network.transfer.AbstractEntityTransferService;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferRequest;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import de.t14d3.rapunzellib.nbt.paper.PaperNbtSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Paper implementation of EntityTransferService using RapunzelLib NBT serialization
 * and network messaging for cross-server entity transfer.
 */
public class PaperEntityTransferService extends AbstractEntityTransferService implements PluginMessageListener {

    private static final String CHANNEL_ENTITY_TRANSFER = "rapunzelcore:entity_transfer";
    private static final long TRANSFER_TIMEOUT_MS = 30000; // 30 seconds

    private final RapunzelPaperCore plugin;
    private final PaperNbtSerializer nbtSerializer;
    private final Map<UUID, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();

    public PaperEntityTransferService(RapunzelPaperCore plugin) {
        super(
            plugin.getLogger(),
            plugin.getMessenger(),
            resolveLocalServerName(plugin),
            plugin.getMessenger() != null && plugin.getMessenger().isConnected()
        );
        this.plugin = plugin;
        this.nbtSerializer = new PaperNbtSerializer();

        // Register plugin messaging channel
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_ENTITY_TRANSFER, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_ENTITY_TRANSFER);

        // Start cleanup task for expired pending transfers
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredTransfers, 20L, 20L);
    }

    @Override
    protected UUID getEntityOwner(UUID entityUuid) {
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null) {
            return null;
        }

        // Check if entity is a pet (has owner)
        if (entity.hasMetadata("pet_owner")) {
            return (UUID) entity.getMetadata("pet_owner").get(0).value();
        }

        return null;
    }

    @Override
    protected String getEntityType(UUID entityUuid) {
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null) {
            return "unknown";
        }
        return entity.getType().name();
    }

    @Override
    protected Map<String, Object> getEntityMetadata(UUID entityUuid) {
        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null) {
            return Map.of();
        }
        return Map.of(
            "entityType", entity.getType().name(),
            "isCustom", entity.hasMetadata("custom_entity")
        );
    }

    @Override
    protected void removeLocalEntity(UUID entityUuid) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity != null) {
                entity.remove();
            }
        });
    }

    @Override
    public CompletableFuture<SerializedEntity> serializeEntity(UUID entityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Entity entity = Bukkit.getEntity(entityUuid);
            if (entity == null || !entity.isValid()) {
                return null;
            }

            try {
                return nbtSerializer.serialize(entity);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to serialize entity", e);
                return null;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<UUID> receiveEntity(EntityTransferRequest request) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                World world = request.targetWorld() != null
                    ? Bukkit.getWorld(request.targetWorld())
                    : Bukkit.getWorlds().get(0);

                if (world == null) {
                    future.complete(null);
                    return;
                }

                Location location = new Location(world, request.targetX(), request.targetY(), request.targetZ(),
                    request.targetYaw(), request.targetPitch());

                // Deserialize NBT data
                SerializedEntity serializedEntity = request.serializedEntity();
                if (serializedEntity == null) {
                    future.complete(null);
                    return;
                }

                // Spawn entity from serialized data
                Entity spawned = nbtSerializer.deserialize(serializedEntity, location);
                if (spawned == null) {
                    future.complete(null);
                    return;
                }

                future.complete(spawned.getUniqueId());

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to spawn entity", e);
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(CHANNEL_ENTITY_TRANSFER)) {
            return;
        }

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String action = input.readUTF();

            switch (action) {
                case "TRANSFER_REQUEST" -> handleIncomingTransfer(input);
                case "TRANSFER_CONFIRM" -> handleTransferConfirmation(input);
                case "TRANSFER_FAILED" -> handleTransferFailure(input);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to process plugin message", e);
        }
    }

    private void handleIncomingTransfer(ByteArrayDataInput input) {
        try {
            String targetServer = input.readUTF();
            if (!targetServer.equals(localServerName)) {
                return;
            }

            UUID originalUuid = UUID.fromString(input.readUTF());
            String entityType = input.readUTF();

            int nbtLength = input.readInt();
            byte[] nbtData = new byte[nbtLength];
            input.readFully(nbtData);

            String sourceServer = input.readUTF();
            String targetWorld = input.readUTF();
            if (targetWorld.isEmpty()) targetWorld = null;
            double targetX = input.readDouble();
            double targetY = input.readDouble();
            double targetZ = input.readDouble();
            float targetYaw = input.readFloat();
            float targetPitch = input.readFloat();
            String ownerUuidStr = input.readUTF();
            UUID ownerUuid = ownerUuidStr.isEmpty() ? null : UUID.fromString(ownerUuidStr);

            // Deserialize entity from bytes
            SerializedEntity serializedEntity = SerializedEntity.fromBase64(new String(nbtData));
            if (serializedEntity == null) {
                sendTransferConfirmation(originalUuid, sourceServer, false);
                return;
            }

            EntityTransferRequest request = EntityTransferRequest.builder()
                .entityUuid(originalUuid)
                .entityType(entityType)
                .sourceServer(sourceServer)
                .targetServer(targetServer)
                .targetWorld(targetWorld)
                .targetX(targetX)
                .targetY(targetY)
                .targetZ(targetZ)
                .targetYaw(targetYaw)
                .targetPitch(targetPitch)
                .serializedEntity(serializedEntity)
                .ownerUuid(ownerUuid)
                .metadata(Map.of())
                .build();

            // Receive and spawn the entity
            receiveEntity(request).thenAccept(newUuid -> {
                if (newUuid != null) {
                    sendTransferConfirmation(originalUuid, sourceServer, true);
                } else {
                    sendTransferConfirmation(originalUuid, sourceServer, false);
                }
            });

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to handle incoming transfer", e);
        }
    }

    private void handleTransferConfirmation(ByteArrayDataInput input) {
        try {
            UUID entityUuid = UUID.fromString(input.readUTF());
            boolean success = input.readBoolean();

            PendingTransfer pending = pendingTransfers.remove(entityUuid);
            if (pending != null && !success) {
                plugin.getLogger().warning("[EntityTransfer] Transfer failed for " + entityUuid + ", attempting recovery");
                receiveEntity(pending.request());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to handle transfer confirmation", e);
        }
    }

    private void handleTransferFailure(ByteArrayDataInput input) {
        try {
            UUID entityUuid = UUID.fromString(input.readUTF());
            String reason = input.readUTF();

            PendingTransfer pending = pendingTransfers.remove(entityUuid);
            if (pending != null) {
                plugin.getLogger().warning("[EntityTransfer] Transfer failed for " + entityUuid + ": " + reason);
                receiveEntity(pending.request());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to handle transfer failure", e);
        }
    }

    private void sendTransferConfirmation(UUID originalUuid, String targetServer, boolean success) {
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("TRANSFER_CONFIRM");
            output.writeUTF(originalUuid.toString());
            output.writeBoolean(success);

            plugin.getMessenger().sendToServer(targetServer, CHANNEL_ENTITY_TRANSFER, java.util.Base64.getEncoder().encodeToString(output.toByteArray()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[EntityTransfer] Failed to send confirmation", e);
        }
    }

    private void cleanupExpiredTransfers() {
        long now = System.currentTimeMillis();
        pendingTransfers.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp() > TRANSFER_TIMEOUT_MS) {
                receiveEntity(entry.getValue().request());
                return true;
            }
            return false;
        });
    }

    private static String resolveLocalServerName(RapunzelPaperCore plugin) {
        String serverName = plugin.getConfiguration().getString("server-name", null);
        if (serverName != null && !serverName.isBlank()) {
            return serverName;
        }
        return Bukkit.getServer().getName();
    }

    public void shutdown() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_ENTITY_TRANSFER);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_ENTITY_TRANSFER);

        for (PendingTransfer pending : pendingTransfers.values()) {
            receiveEntity(pending.request());
        }
        pendingTransfers.clear();
    }

    private record PendingTransfer(UUID entityUuid, long timestamp, EntityTransferRequest request) {}
}
