package de.t14d3.rapunzelcore.modules.pets;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Listener interface for handling pet owner server transfer events.
 * Implementations of this interface are notified when a pet owner changes
 * servers or disconnects from the network, allowing for appropriate pet
 * transfer or cleanup actions.
 *
 * <p>This interface is designed to be implemented by platform-specific code
 * that handles player connection events and coordinates pet transfers when
 * owners move between servers in the network.</p>
 *
 * <p>Typical usage involves registering an implementation with the pet
 * transfer system to receive callbacks when:</p>
 * <ul>
 *   <li>A player switches to a different server (triggers pet transfer)</li>
 *   <li>A player disconnects from the network (triggers pet cleanup)</li>
 * </ul>
 *
 * @see PetTransferService
 * @see PetTransferAdapter
 */
public interface PetOwnerTransferListener {

    /**
     * Called when a pet owner changes to a different server in the network.
     * Implementations should initiate the transfer of all pets owned by
     * the specified player to the target server.
     *
     * <p>This method is typically invoked by platform-specific event handlers
     * when a player is detected switching servers (e.g., through proxy
     * server events or network messages).</p>
     *
     * @param ownerUuid the UUID of the pet owner changing servers
     * @param targetServer the name of the server the owner is transferring to
     */
    void onOwnerChangeServer(@NotNull UUID ownerUuid, @NotNull String targetServer);

    /**
     * Called when a pet owner disconnects from the network entirely.
     * Implementations should handle cleanup of pets or transfer them to
     * a safe location (such as a stable or pet storage system).
     *
     * <p>This method is typically invoked by platform-specific event handlers
     * when a player disconnects from the proxy or network. The implementation
     * may choose to:</p>
     * <ul>
     *   <li>Despawn pets and store them for later retrieval</li>
     *   <li>Transfer pets to a default server or safe zone</li>
     *   <li>Leave pets in place if the server configuration allows it</li>
     * </ul>
     *
     * @param ownerUuid the UUID of the pet owner who disconnected
     */
    void onOwnerDisconnect(@NotNull UUID ownerUuid);
}
