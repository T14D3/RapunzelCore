package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Pet;
import de.t14d3.rapunzelcore.modules.pets.network.PetTransferPayload;
import de.t14d3.rapunzelcore.modules.pets.network.PetTransferRequest;
import de.t14d3.rapunzelcore.network.transfer.EntityTransferService;
import de.t14d3.rapunzellib.nbt.SerializedEntity;
import de.t14d3.rapunzellib.nbt.paper.PaperNbtSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * Paper implementation of PetTransferService using EntityTransferService for cross-server transfers.
 */
public class PaperPetTransferService implements PetTransferService, PluginMessageListener {

    private static final String CHANNEL_PET_TRANSFER = "rapunzelcore:pet_transfer";
    private static final long TRANSFER_TIMEOUT_MS = 30000;

    private final RapunzelPaperCore plugin;
    private final EntityTransferService entityTransferService;
    private final PaperNbtSerializer nbtSerializer;
    private final PetRepository petRepository;
    private final String localServerName;
    private final Map<UUID, PendingPetTransfer> pendingTransfers = new ConcurrentHashMap<>();

    public PaperPetTransferService(RapunzelPaperCore plugin, EntityTransferService entityTransferService, PetRepository petRepository) {
        this.plugin = plugin;
        this.entityTransferService = entityTransferService;
        this.nbtSerializer = new PaperNbtSerializer();
        this.petRepository = petRepository;
        this.localServerName = resolveLocalServerName();

        // Register plugin messaging channel
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_PET_TRANSFER, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_PET_TRANSFER);

        // Start cleanup task
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredTransfers, 20L, 20L);
    }

    @Override
    public CompletableFuture<Boolean> transferPet(@NotNull UUID petId, @NotNull UUID ownerUuid,
                                                   @NotNull String targetServer, @Nullable String targetWorld,
                                                   double targetX, double targetY, double targetZ) {
        // Build the request
        PetTransferRequest.Builder builder = PetTransferRequest.builder()
            .petId(petId)
            .ownerUuid(ownerUuid)
            .targetServer(targetServer)
            .targetLocation(targetX, targetY, targetZ)
            .petType("UNKNOWN");

        if (targetWorld != null) {
            builder.targetWorld(targetWorld);
        }

        return transferPet(builder.build());
    }

    @Override
    public CompletableFuture<Boolean> transferPet(@NotNull PetTransferRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if pet entity exists
                Entity entity = Bukkit.getEntity(request.petId());
                if (entity == null || !entity.isValid()) {
                    plugin.getLogger().warning("[PetTransfer] Pet entity not found: " + request.petId());
                    return false;
                }

                // Verify it's a tameable pet
                if (!(entity instanceof Tameable tameable)) {
                    plugin.getLogger().warning("[PetTransfer] Entity is not a tameable pet: " + request.petId());
                    return false;
                }

                // Verify ownership
                if (tameable.getOwner() == null || !tameable.getOwner().getUniqueId().equals(request.ownerUuid())) {
                    plugin.getLogger().warning("[PetTransfer] Pet ownership mismatch: " + request.petId());
                    return false;
                }

                // Serialize the pet
                de.t14d3.rapunzellib.nbt.SerializedEntity serializedData = serializePetInternal(entity, request).get();
                if (serializedData == null) {
                    plugin.getLogger().warning("[PetTransfer] Failed to serialize pet: " + request.petId());
                    return false;
                }

                // Check network availability
                if (!isTransferEnabled()) {
                    plugin.getLogger().warning("[PetTransfer] Network not available for transfer");
                    return false;
                }

                // Send transfer request
                PetTransferPayload payload = PetTransferPayload.fromRequest(request, localServerName);
                boolean sent = sendTransferRequest(payload);
                if (!sent) {
                    plugin.getLogger().warning("[PetTransfer] Failed to send transfer request to " + request.targetServer());
                    return false;
                }

                // Mark transfer as pending
                pendingTransfers.put(request.petId(), new PendingPetTransfer(
                    request.petId(),
                    request.ownerUuid(),
                    System.currentTimeMillis(),
                    serializedData
                ));

                // Update pet record with new server
                Pet pet = petRepository.getByEntityUuid(request.petId());
                if (pet != null) {
                    pet.setLastServer(request.targetServer());
                    petRepository.updatePet(pet);
                }

                // Remove entity from this server
                Bukkit.getScheduler().runTask(plugin, () -> entity.remove());

                plugin.getLogger().info("[PetTransfer] Pet " + request.petId() + " transferred to " + request.targetServer());
                return true;

            } catch (Exception e) {
                plugin.getLogger().warning("[PetTransfer] Failed to transfer pet: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> receivePet(@NotNull PetTransferRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                World world = request.targetWorld() != null
                    ? Bukkit.getWorld(request.targetWorld())
                    : Bukkit.getWorlds().get(0);

                if (world == null) {
                    plugin.getLogger().warning("[PetTransfer] Target world not found: " + request.targetWorld());
                    return false;
                }

                Location location = new Location(world, request.targetX(), request.targetY(), request.targetZ());

                // Deserialize and spawn the pet
                SerializedEntity serializedEntity = request.serializedEntity();
                if (serializedEntity == null) {
                    plugin.getLogger().warning("[PetTransfer] Failed to deserialize pet entity");
                    return false;
                }

                Entity spawned = nbtSerializer.deserialize(serializedEntity, location);
                if (spawned == null) {
                    plugin.getLogger().warning("[PetTransfer] Failed to spawn pet entity");
                    return false;
                }

                // Restore ownership
                if (spawned instanceof Tameable tameable) {
                    Player owner = Bukkit.getPlayer(request.ownerUuid());
                    if (owner != null) {
                        tameable.setOwner(owner);
                    } else {
                        // Owner is offline, set owner UUID directly
                        // This requires reflection or API support
                        tameable.setOwner(null); // Will need to be restored when owner logs in
                    }
                }

                // Update pet record with new entity UUID and server
                Pet pet = petRepository.findById(request.petId());
                if (pet != null) {
                    pet.setEntityUuid(spawned.getUniqueId().toString());
                    pet.setLastServer(localServerName);
                    petRepository.updatePet(pet);
                }

                plugin.getLogger().info("[PetTransfer] Pet received from " + request.targetServer() + ", new UUID: " + spawned.getUniqueId());
                return true;

            } catch (Exception e) {
                plugin.getLogger().warning("[PetTransfer] Failed to receive pet: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<de.t14d3.rapunzellib.nbt.SerializedEntity> serializePet(@NotNull UUID petId) {
        return CompletableFuture.supplyAsync(() -> {
            Entity entity = Bukkit.getEntity(petId);
            if (entity == null || !entity.isValid()) {
                return null;
            }

            try {
                SerializedEntity serialized = nbtSerializer.serialize(entity);
                if (serialized == null) {
                    return null;
                }
                return serialized;
            } catch (Exception e) {
                plugin.getLogger().warning("[PetTransfer] Failed to serialize pet: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public boolean isTransferEnabled() {
        return entityTransferService != null && entityTransferService.isTransferEnabled();
    }

    @Override
    @NotNull
    public String getLocalServerName() {
        return localServerName;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equals(CHANNEL_PET_TRANSFER)) {
            return;
        }

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String action = input.readUTF();

            if ("PET_TRANSFER".equals(action)) {
                handleIncomingPetTransfer(input);
            } else if ("PET_TRANSFER_CONFIRM".equals(action)) {
                handleTransferConfirmation(input);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PetTransfer] Failed to process plugin message: " + e.getMessage());
        }
    }

    /**
     * Transfers a pet when its owner transfers to another server.
     *
     * @param ownerUuid the owner UUID
     * @param targetServer the target server name
     * @param targetWorld the target world name
     * @param targetX target X coordinate
     * @param targetY target Y coordinate
     * @param targetZ target Z coordinate
     * @return a future that completes when all pets are transferred
     */
    public CompletableFuture<Boolean> transferPetsWithOwner(@NotNull UUID ownerUuid, @NotNull String targetServer,
                                                             @Nullable String targetWorld, double targetX, double targetY, double targetZ) {
        return CompletableFuture.supplyAsync(() -> {
            boolean allSuccess = true;

            // Get all pets owned by this player
            for (Pet pet : petRepository.getByOwner(ownerUuid)) {
                // Check if pet is on this server
                if (!localServerName.equals(pet.getLastServer())) {
                    continue; // Pet is already on another server
                }

                Entity entity = Bukkit.getEntity(UUID.fromString(pet.getEntityUuid()));
                if (entity == null || !entity.isValid()) {
                    continue; // Pet not loaded or invalid
                }

                // Transfer the pet
                PetTransferRequest request = PetTransferRequest.builder()
                    .petId(UUID.fromString(pet.getEntityUuid()))
                    .ownerUuid(ownerUuid)
                    .targetServer(targetServer)
                    .targetWorld(targetWorld)
                    .targetLocation(targetX, targetY, targetZ)
                    .serializedEntity(new de.t14d3.rapunzellib.nbt.SerializedEntity(
                    entity.getType().name(),
                    new byte[0],
                    List.of(),
                    entity.getUniqueId(),
                    Instant.now(),
                    Map.of()
                ))
                    .petType(pet.getEntityType())
                    .customName(pet.getCustomName())
                    .build();

                Boolean success = transferPet(request).join();
                if (!success) {
                    allSuccess = false;
                    plugin.getLogger().warning("[PetTransfer] Failed to transfer pet " + pet.getId() + " with owner");
                }
            }

            return allSuccess;
        });
    }

    private CompletableFuture<de.t14d3.rapunzellib.nbt.SerializedEntity> serializePetInternal(Entity entity, PetTransferRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SerializedEntity serialized = nbtSerializer.serialize(entity);
                if (serialized == null) {
                    return null;
                }

                return new de.t14d3.rapunzellib.nbt.SerializedEntity(
                    entity.getType().name(),
                    serialized.nbtData(),
                    List.of(),
                    entity.getUniqueId(),
                    Instant.now(),
                    Map.of(
                        "petType", request.petType(),
                        "customName", request.customName() != null ? request.customName() : "",
                        "ownerUuid", request.ownerUuid() != null ? request.ownerUuid().toString() : "",
                        "sourceServer", localServerName,
                        "targetServer", request.targetServer(),
                        "targetWorld", request.targetWorld() != null ? request.targetWorld() : "",
                        "targetX", request.targetX(),
                        "targetY", request.targetY(),
                        "targetZ", request.targetZ()
                    )
                );

            } catch (Exception e) {
                plugin.getLogger().warning("[PetTransfer] Serialization failed: " + e.getMessage());
                return null;
            }
        });
    }

    private boolean sendTransferRequest(PetTransferPayload payload) {
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("PET_TRANSFER");
            output.writeUTF(payload.targetServer());
            output.writeUTF(payload.petId().toString());
            output.writeUTF(payload.ownerUuid().toString());
            output.writeUTF(payload.petType());
            output.writeUTF(payload.customName() != null ? payload.customName() : "");
            output.writeUTF(payload.targetWorld() != null ? payload.targetWorld() : "");
            output.writeDouble(payload.targetX());
            output.writeDouble(payload.targetY());
            output.writeDouble(payload.targetZ());
            output.writeUTF(payload.sourceServer());

            // Serialize entity data
            byte[] nbtData = payload.serializedEntity().toBase64().getBytes();
            output.writeInt(nbtData.length);
            output.write(nbtData);

            plugin.getMessenger().sendToServer(payload.targetServer(), CHANNEL_PET_TRANSFER, java.util.Base64.getEncoder().encodeToString(output.toByteArray()));
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[PetTransfer] Failed to send transfer request: " + e.getMessage());
            return false;
        }
    }

    private void handleIncomingPetTransfer(ByteArrayDataInput input) {
        try {
            String targetServer = input.readUTF();
            if (!targetServer.equals(localServerName)) {
                return;
            }

            UUID petId = UUID.fromString(input.readUTF());
            UUID ownerUuid = UUID.fromString(input.readUTF());
            String petType = input.readUTF();
            String customName = input.readUTF();
            if (customName.isEmpty()) customName = null;
            String targetWorld = input.readUTF();
            if (targetWorld.isEmpty()) targetWorld = null;
            double targetX = input.readDouble();
            double targetY = input.readDouble();
            double targetZ = input.readDouble();
            String sourceServer = input.readUTF();

            int nbtLength = input.readInt();
            byte[] nbtData = new byte[nbtLength];
            input.readFully(nbtData);

            de.t14d3.rapunzellib.nbt.SerializedEntity serializedEntity =
                de.t14d3.rapunzellib.nbt.SerializedEntity.fromBase64(new String(nbtData));

            PetTransferRequest request = PetTransferRequest.builder()
                .petId(petId)
                .ownerUuid(ownerUuid)
                .targetServer(targetServer)
                .targetWorld(targetWorld)
                .targetLocation(targetX, targetY, targetZ)
                .serializedEntity(serializedEntity)
                .petType(petType)
                .customName(customName)
                .build();

            receivePet(request).thenAccept(success -> {
                sendTransferConfirmation(petId, sourceServer, success);
            });

        } catch (Exception e) {
            plugin.getLogger().warning("[PetTransfer] Failed to handle incoming transfer: " + e.getMessage());
        }
    }

    private void handleTransferConfirmation(ByteArrayDataInput input) {
        try {
            UUID petId = UUID.fromString(input.readUTF());
            boolean success = input.readBoolean();

            PendingPetTransfer pending = pendingTransfers.remove(petId);
            if (pending != null && !success) {
                plugin.getLogger().warning("[PetTransfer] Transfer failed for " + petId + ", attempting recovery");
                // Attempt to respawn the pet on this server
                // This would need the original entity data
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PetTransfer] Failed to handle transfer confirmation: " + e.getMessage());
        }
    }


    private void sendTransferConfirmation(UUID petId, String targetServer, boolean success) {
        try {
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            output.writeUTF("PET_TRANSFER_CONFIRM");
            output.writeUTF(petId.toString());
            output.writeBoolean(success);

            plugin.getMessenger().sendToServer(targetServer, CHANNEL_PET_TRANSFER, java.util.Base64.getEncoder().encodeToString(output.toByteArray()));
        } catch (Exception e) {
            plugin.getLogger().warning("[PetTransfer] Failed to send confirmation: " + e.getMessage());
        }
    }

    private void cleanupExpiredTransfers() {
        long now = System.currentTimeMillis();
        pendingTransfers.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp() > TRANSFER_TIMEOUT_MS) {
                plugin.getLogger().warning("[PetTransfer] Transfer timed out for pet " + entry.getKey());
                return true;
            }
            return false;
        });
    }

    private String resolveLocalServerName() {
        String serverName = plugin.getConfiguration().getString("server-name", null);
        if (serverName != null && !serverName.isBlank()) {
            return serverName;
        }
        return Bukkit.getServer().getName();
    }

    public void shutdown() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_PET_TRANSFER);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_PET_TRANSFER);
        pendingTransfers.clear();
    }

    private record PendingPetTransfer(UUID petId, UUID ownerUuid, long timestamp, de.t14d3.rapunzellib.nbt.SerializedEntity data) {}
}
