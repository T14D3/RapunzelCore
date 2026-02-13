package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Pet;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Command to revoke access to a pet.
 */
public class PetDenyCommand {

    private static final int NEARBY_RADIUS = 5;

    private final RapunzelPaperCore core;
    private final PetModule module;
    private final PetRepository repository;

    public PetDenyCommand(RapunzelPaperCore core, PetModule module) {
        this.core = core;
        this.module = module;
        this.repository = module.getRepository();
    }

    /**
     * Revokes a player's access to a pet.
     * @param owner the pet owner
     * @param targetPlayerName the player to revoke access from
     * @param petName optional pet name (uses nearest if null)
     */
    public void revokeAccess(Player owner, String targetPlayerName, String petName) {
        // Find target player
        UUID targetUuid = resolvePlayerUuid(targetPlayerName);
        if (targetUuid == null) {
            owner.sendMessage(core.getMessageHandler().getMessage("general.error.player.invalid", targetPlayerName));
            return;
        }

        Pet pet;
        if (petName != null) {
            pet = findPetByName(owner, petName);
            if (pet == null) {
                owner.sendMessage(core.getMessageHandler().getMessage("pets.error.not_found", petName));
                return;
            }
        } else {
            pet = findNearestOwnedPet(owner);
            if (pet == null) {
                owner.sendMessage(core.getMessageHandler().getMessage("pets.error.no_pet_nearby"));
                return;
            }
        }

        // Check ownership
        if (!pet.getOwnerUuid().equals(owner.getUniqueId().toString())) {
            owner.sendMessage(core.getMessageHandler().getMessage("pets.error.not_owner"));
            return;
        }

        // Check if has access
        if (!repository.isAllowed(pet, targetUuid)) {
            String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
            owner.sendMessage(core.getMessageHandler().getMessage("pets.deny.no_access", targetName));
            return;
        }

        // Owner cannot deny themselves
        if (targetUuid.equals(owner.getUniqueId())) {
            owner.sendMessage(core.getMessageHandler().getMessage("pets.deny.self"));
            return;
        }

        // Revoke access
        repository.removeAllowedPlayer(pet, targetUuid);

        // Notify players
        String petDisplayName = pet.getCustomName() != null ? pet.getCustomName() : formatEntityType(pet.getEntityType());
        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        
        owner.sendMessage(core.getMessageHandler().getMessage("pets.deny.revoked", targetName, petDisplayName));

        // Notify target if online
        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            target.sendMessage(core.getMessageHandler().getMessage("pets.deny.notify_revoked",
                owner.getName(), petDisplayName));
        }

        // Visual feedback
        Entity entity = Bukkit.getEntity(UUID.fromString(pet.getEntityUuid()));
        if (entity != null && entity.isValid()) {
            entity.getWorld().spawnParticle(
                org.bukkit.Particle.SMOKE,
                entity.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.1
            );
        }
    }

    /**
     * Resolves a player name to UUID (online or offline).
     */
    private UUID resolvePlayerUuid(String name) {
        // Try online player first
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online.getUniqueId();
        }

        // Try offline player
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }

        return null;
    }

    /**
     * Finds the nearest pet owned by the player.
     */
    private Pet findNearestOwnedPet(Player player) {
        Pet nearestPet = null;
        double nearestDistance = Double.MAX_VALUE;
        String playerUuid = player.getUniqueId().toString();

        for (Entity entity : player.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)) {
            Pet pet = module.getPet(entity.getUniqueId());
            if (pet != null && pet.getOwnerUuid().equals(playerUuid)) {
                double distance = player.getLocation().distanceSquared(entity.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPet = pet;
                }
            }
        }

        return nearestPet;
    }

    /**
     * Finds a pet by name owned by the player.
     */
    private Pet findPetByName(Player player, String name) {
        String searchName = name.toLowerCase();
        
        // Check owned pets
        List<Pet> ownedPets = repository.getByOwner(player.getUniqueId());
        for (Pet pet : ownedPets) {
            String petDisplayName = pet.getCustomName() != null ? pet.getCustomName().toLowerCase() : "";
            String petType = pet.getEntityType().toLowerCase();
            if (petDisplayName.contains(searchName) || petType.contains(searchName)) {
                return pet;
            }
        }
        
        // Check nearby pets
        for (Entity entity : player.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)) {
            Pet pet = module.getPet(entity.getUniqueId());
            if (pet != null && pet.getOwnerUuid().equals(player.getUniqueId().toString())) {
                String petDisplayName = pet.getCustomName() != null ? pet.getCustomName().toLowerCase() : "";
                String petType = pet.getEntityType().toLowerCase();
                if (petDisplayName.contains(searchName) || petType.contains(searchName)) {
                    return pet;
                }
            }
        }
        
        return null;
    }

    private String formatEntityType(String type) {
        return java.util.Arrays.stream(type.toLowerCase().split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(java.util.stream.Collectors.joining(" "));
    }
}
