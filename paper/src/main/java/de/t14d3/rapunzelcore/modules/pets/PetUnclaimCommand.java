package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Pet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Command to release/unclaim a pet.
 */
public class PetUnclaimCommand {

    private static final int NEARBY_RADIUS = 5;

    private final RapunzelPaperCore core;
    private final PetModule module;
    private final PetRepository repository;

    public PetUnclaimCommand(RapunzelPaperCore core, PetModule module) {
        this.core = core;
        this.module = module;
        this.repository = module.getRepository();
    }

    /**
     * Unclaims a pet by name or the nearest pet if no name provided.
     * @param player the player unclaiming
     * @param petName optional pet name
     */
    public void unclaimPet(Player player, String petName) {
        Pet pet;
        
        if (petName != null) {
            // Find by name
            pet = findPetByName(player, petName);
            if (pet == null) {
                player.sendMessage(core.getMessageHandler().getMessage("pets.unclaim.not_found", petName));
                return;
            }
        } else {
            // Find nearest pet owned by player
            pet = findNearestOwnedPet(player);
            if (pet == null) {
                player.sendMessage(core.getMessageHandler().getMessage("pets.unclaim.no_pet_nearby"));
                return;
            }
        }

        // Check ownership
        if (!pet.getOwnerUuid().equals(player.getUniqueId().toString())) {
            // Allow admins to unclaim any pet
            if (!player.hasPermission("rapunzelcore.pet.admin")) {
                player.sendMessage(core.getMessageHandler().getMessage("pets.error.not_owner"));
                return;
            }
        }

        // Get entity for visual feedback
        UUID entityUuid = UUID.fromString(pet.getEntityUuid());
        Entity entity = Bukkit.getEntity(entityUuid);
        String displayName = pet.getCustomName() != null ? pet.getCustomName() : formatEntityType(pet.getEntityType());

        // Delete from database
        repository.deletePet(pet);
        
        // Remove from module tracking
        module.untrackPet(entityUuid);

        // Visual feedback
        player.sendMessage(core.getMessageHandler().getMessage("pets.unclaim.success", displayName));
        
        // Play particles if entity exists
        if (entity != null && entity.isValid()) {
            entity.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                entity.getLocation().add(0, 1, 0),
                5, 0.5, 0.5, 0.5, 0
            );
        }
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
        String playerUuid = player.getUniqueId().toString();
        
        // First check owned pets
        List<Pet> ownedPets = repository.getByOwner(player.getUniqueId());
        for (Pet pet : ownedPets) {
            String petDisplayName = pet.getCustomName() != null ? pet.getCustomName().toLowerCase() : "";
            String petType = pet.getEntityType().toLowerCase();
            if (petDisplayName.contains(searchName) || petType.contains(searchName)) {
                return pet;
            }
        }
        
        // Then check nearby pets (for admin override)
        if (player.hasPermission("rapunzelcore.pet.admin")) {
            for (Entity entity : player.getNearbyEntities(NEARBY_RADIUS * 2, NEARBY_RADIUS * 2, NEARBY_RADIUS * 2)) {
                Pet pet = module.getPet(entity.getUniqueId());
                if (pet != null) {
                    String petDisplayName = pet.getCustomName() != null ? pet.getCustomName().toLowerCase() : "";
                    String petType = pet.getEntityType().toLowerCase();
                    if (petDisplayName.contains(searchName) || petType.contains(searchName)) {
                        return pet;
                    }
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
