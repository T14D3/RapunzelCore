package de.t14d3.rapunzelcore.modules.pets;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.database.entities.Pet;
import de.t14d3.spool.repository.EntityRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for Pet entities with caching and database operations.
 */
public class PetRepository extends EntityRepository<Pet> {
    private static final PetRepository instance = new PetRepository();
    private static final Gson gson = new Gson();

    // Cache: entity UUID -> Pet
    private final Map<UUID, Pet> petsByEntityUuid = new ConcurrentHashMap<>();
    // Cache: owner UUID -> List of pets
    private final Map<UUID, List<Pet>> petsByOwner = new ConcurrentHashMap<>();

    public PetRepository() {
        super(CoreDatabase.getEntityManager(), Pet.class);
        loadAllPets();
    }

    public static PetRepository getInstance() {
        return instance;
    }

    /**
     * Loads all pets from the database into the cache.
     */
    private void loadAllPets() {
        CoreDatabase.runLocked(() -> {
            List<Pet> allPets = findAll();
            for (Pet pet : allPets) {
                if (pet != null && pet.getEntityUuid() != null) {
                    cachePet(pet);
                }
            }
        });
    }

    /**
     * Caches a pet in both entity and owner maps.
     */
    private void cachePet(Pet pet) {
        if (pet == null || pet.getEntityUuid() == null) return;
        
        UUID entityUuid = UUID.fromString(pet.getEntityUuid());
        petsByEntityUuid.put(entityUuid, pet);
        
        if (pet.getOwnerUuid() != null) {
            UUID ownerUuid = UUID.fromString(pet.getOwnerUuid());
            petsByOwner.computeIfAbsent(ownerUuid, k -> new ArrayList<>()).add(pet);
        }
    }

    /**
     * Removes a pet from the cache.
     */
    private void uncachePet(Pet pet) {
        if (pet == null) return;
        
        if (pet.getEntityUuid() != null) {
            petsByEntityUuid.remove(UUID.fromString(pet.getEntityUuid()));
        }
        
        if (pet.getOwnerUuid() != null) {
            List<Pet> ownerPets = petsByOwner.get(UUID.fromString(pet.getOwnerUuid()));
            if (ownerPets != null) {
                ownerPets.removeIf(p -> p.getId() != null && p.getId().equals(pet.getId()));
            }
        }
    }

    /**
     * Gets a pet by its entity UUID.
     * @param entityUuid the entity UUID
     * @return the Pet, or null if not found
     */
    public Pet getByEntityUuid(UUID entityUuid) {
        if (entityUuid == null) return null;
        return petsByEntityUuid.get(entityUuid);
    }

    /**
     * Gets all pets owned by a player.
     * @param ownerUuid the owner's UUID
     * @return list of pets
     */
    public List<Pet> getByOwner(UUID ownerUuid) {
        if (ownerUuid == null) return List.of();
        List<Pet> pets = petsByOwner.get(ownerUuid);
        return pets != null ? List.copyOf(pets) : List.of();
    }

    /**
     * Gets a pet by owner and custom name.
     * @param ownerUuid the owner's UUID
     * @param name the custom name
     * @return the Pet, or null if not found
     */
    public Pet getByOwnerAndName(UUID ownerUuid, String name) {
        if (ownerUuid == null || name == null) return null;
        List<Pet> pets = petsByOwner.get(ownerUuid);
        if (pets == null) return null;
        
        String searchName = name.toLowerCase();
        for (Pet pet : pets) {
            if (pet.getCustomName() != null && pet.getCustomName().toLowerCase().contains(searchName)) {
                return pet;
            }
        }
        return null;
    }

    /**
     * Creates a new pet record.
     * @param ownerUuid the owner's UUID
     * @param entityUuid the entity's UUID
     * @param entityType the entity type
     * @param customName the custom name (can be null)
     * @return the created Pet
     */
    public CompletableFuture<Pet> createPet(UUID ownerUuid, UUID entityUuid, String entityType, String customName) {
        return CompletableFuture.supplyAsync(() -> CoreDatabase.locked(() -> {
            Pet pet = new Pet();
            pet.setOwnerUuid(ownerUuid.toString());
            pet.setEntityUuid(entityUuid.toString());
            pet.setEntityType(entityType);
            pet.setCustomName(customName);
            pet.setAllowedPlayers("[]");
            pet.setProtected(true);
            pet.setLastServer(getLocalServerName());
            
            save(pet);
            CoreDatabase.getEntityManager().flush();
            
            cachePet(pet);
            return pet;
        }), CoreDatabase.supplyAsync(() -> null).defaultExecutor());
    }

    /**
     * Deletes a pet from the database and cache.
     * @param pet the pet to delete
     */
    public void deletePet(Pet pet) {
        if (pet == null) return;
        CoreDatabase.runLocked(() -> {
            uncachePet(pet);
            delete(pet);
        });
    }

    /**
     * Updates a pet in the database and cache.
     * @param pet the pet to update
     */
    public void updatePet(Pet pet) {
        if (pet == null) return;
        CoreDatabase.runLocked(() -> {
            pet.setLastSeenAt(System.currentTimeMillis());
            save(pet);
            CoreDatabase.flushAsync();
        });
    }

    /**
     * Gets the list of allowed player UUIDs.
     * @param pet the pet
     * @return set of allowed player UUIDs
     */
    public Set<UUID> getAllowedPlayers(Pet pet) {
        if (pet == null || pet.getAllowedPlayers() == null) return new HashSet<>();
        try {
            List<String> uuidStrings = gson.fromJson(pet.getAllowedPlayers(), new TypeToken<List<String>>(){}.getType());
            if (uuidStrings == null) return new HashSet<>();
            Set<UUID> uuids = new HashSet<>();
            for (String s : uuidStrings) {
                try {
                    uuids.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
            return uuids;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    /**
     * Sets the list of allowed player UUIDs.
     * @param pet the pet
     * @param allowedPlayers set of allowed player UUIDs
     */
    public void setAllowedPlayers(Pet pet, Set<UUID> allowedPlayers) {
        if (pet == null) return;
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : allowedPlayers) {
            uuidStrings.add(uuid.toString());
        }
        pet.setAllowedPlayers(gson.toJson(uuidStrings));
    }

    /**
     * Adds a player to the allowed list.
     * @param pet the pet
     * @param playerUuid the player UUID to add
     */
    public void addAllowedPlayer(Pet pet, UUID playerUuid) {
        if (pet == null || playerUuid == null) return;
        Set<UUID> allowed = getAllowedPlayers(pet);
        allowed.add(playerUuid);
        setAllowedPlayers(pet, allowed);
        updatePet(pet);
    }

    /**
     * Removes a player from the allowed list.
     * @param pet the pet
     * @param playerUuid the player UUID to remove
     */
    public void removeAllowedPlayer(Pet pet, UUID playerUuid) {
        if (pet == null || playerUuid == null) return;
        Set<UUID> allowed = getAllowedPlayers(pet);
        allowed.remove(playerUuid);
        setAllowedPlayers(pet, allowed);
        updatePet(pet);
    }

    /**
     * Checks if a player is allowed to interact with a pet.
     * @param pet the pet
     * @param playerUuid the player UUID
     * @return true if allowed
     */
    public boolean isAllowed(Pet pet, UUID playerUuid) {
        if (pet == null || playerUuid == null) return false;
        if (pet.getOwnerUuid().equals(playerUuid.toString())) return true;
        return getAllowedPlayers(pet).contains(playerUuid);
    }

    /**
     * Updates the last known location of a pet.
     * @param entityUuid the entity UUID
     * @param serverName the server name
     */
    public void updateLocation(UUID entityUuid, String serverName) {
        Pet pet = getByEntityUuid(entityUuid);
        if (pet != null) {
            pet.setLastSeenAt(System.currentTimeMillis());
            pet.setLastServer(serverName);
            updatePet(pet);
        }
    }

    private String getLocalServerName() {
        // This will be set by the module
        return "unknown";
    }

    /**
     * Refreshes the cache for a specific pet from the database.
     * @param petId the pet ID
     */
    public void refreshFromDb(Long petId) {
        if (petId == null) return;
        CoreDatabase.runLocked(() -> {
            Pet pet = findById(petId);
            if (pet != null) {
                uncachePet(petsByEntityUuid.get(UUID.fromString(pet.getEntityUuid())));
                cachePet(pet);
            }
        });
    }

    /**
     * Gets all cached pets.
     * @return collection of all pets
     */
    public Collection<Pet> getAllPets() {
        return List.copyOf(petsByEntityUuid.values());
    }
}
