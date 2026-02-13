package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.database.entities.Pet;
import de.t14d3.rapunzelcore.modules.commands.Command;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module for pet management including protection, tracking, and cross-server sync.
 */
public class PetModule implements Module, Listener {
    private RapunzelCore core;
    private boolean enabled = false;

    private PetRepository repository;
    private BukkitTask locationSyncTask;
    private PetCommand petCommand;

    // Track pets loaded in memory: entity UUID -> Pet
    private final Map<UUID, Pet> trackedPets = new ConcurrentHashMap<>();

    public Environment getEnvironment() {
        return Environment.PAPER;
    }

    public String getName() {
        return "pets";
    }

    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.pet", "true"),
                Map.entry("rapunzelcore.pet.unclaim", "true"),
                Map.entry("rapunzelcore.pet.access", "true"),
                Map.entry("rapunzelcore.pet.deny", "true"),
                Map.entry("rapunzelcore.pet.admin", "op")
        );
    }

    public void enable(RapunzelCore core) {
        this.core = core;
        this.enabled = true;
        // Initialize repository
        this.repository = PetRepository.getInstance();

        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, ((RapunzelPaperCore) core));

        // Register commands
        this.petCommand = new PetCommand(((RapunzelPaperCore) core), this);

        // Start location sync task every 20 ticks (1 second)
        locationSyncTask = Bukkit.getScheduler().runTaskTimer(((RapunzelPaperCore) core), () -> {
        String localServer = getLocalServerName();
        for (Map.Entry<UUID, Pet> entry : trackedPets.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null && entity.isValid()) {
                Pet pet = entry.getValue();
                pet.setLastSeenAt(System.currentTimeMillis());
                if (!localServer.equals(pet.getLastServer())) {
                    pet.setLastServer(localServer);
                }
                repository.updatePet(pet);
            }
        }
    }, 20L, 20L);

        // Load existing pets that are on this server
        loadLocalPets();

        ((RapunzelPaperCore) core).getLogger().info("[Pets] Module enabled with " + trackedPets.size() + " tracked pets");
    }

    public void disable() {
        this.enabled = false;
        if (locationSyncTask != null) {
            locationSyncTask.cancel();
            locationSyncTask = null;
        }

        if (petCommand != null) {
            petCommand.unregister();
            petCommand = null;
        }
        trackedPets.clear();
    }

    /**
     * Loads pets that are present on this server.
     */
    private void loadLocalPets() {
        String localServer = getLocalServerName();
        for (Pet pet : repository.getAllPets()) {
            if (localServer.equals(pet.getLastServer())) {
                try {
                    UUID entityUuid = UUID.fromString(pet.getEntityUuid());
                    trackedPets.put(entityUuid, pet);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /**
     * Syncs pet locations to the database periodically.
     */
    private void syncLocations() {
        String localServer = getLocalServerName();
        for (Map.Entry<UUID, Pet> entry : trackedPets.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity != null && entity.isValid()) {
                Pet pet = entry.getValue();
                pet.setLastSeenAt(System.currentTimeMillis());
                if (!localServer.equals(pet.getLastServer())) {
                    pet.setLastServer(localServer);
                }
                repository.updatePet(pet);
            } else {
                // Entity no longer exists, remove from tracking
                trackedPets.remove(entry.getKey());
            }
        }
    }

    private String getLocalServerName() {
        // Try to get from core.getConfiguration() or network service
        String serverName = core.getConfiguration().getString("server-name", null);
        if (serverName != null && !serverName.isBlank()) {
            return serverName;
        }
        return Bukkit.getServer().getName();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Pet pet = trackedPets.get(event.getEntity().getUniqueId());
        if (pet == null) {
            // Check if this entity is a pet but not yet tracked
            pet = repository.getByEntityUuid(event.getEntity().getUniqueId());
            if (pet != null) {
                trackedPets.put(event.getEntity().getUniqueId(), pet);
            }
        }

        if (pet == null || !pet.isProtected()) return;

        // Allow damage from non-players (environment, mobs)
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        UUID damagerUuid = damager.getUniqueId();

        // Owner can always damage their own pet
        if (pet.getOwnerUuid().equals(damagerUuid.toString())) {
            return;
        }

        // Check if player has access
        if (!repository.isAllowed(pet, damagerUuid)) {
            event.setCancelled(true);
            damager.sendMessage(((RapunzelPaperCore) core).getMessageHandler().getMessage("pets.error.no_access"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player owner)) return;

        Entity entity = event.getEntity();
        String customName = null;
        if (entity.customName() != null) {
            customName = entity.customName().toString();
        }

        repository.createPet(
                owner.getUniqueId(),
                entity.getUniqueId(),
                entity.getType().name(),
                customName
        ).thenAccept(pet -> Bukkit.getScheduler().runTask(((RapunzelPaperCore) core), () -> {
            if (pet != null) {
                trackedPets.put(entity.getUniqueId(), pet);
                owner.sendMessage(((RapunzelPaperCore) core).getMessageHandler().getMessage("pets.tamed.success",
                        pet.getEntityType()));
            }
        }));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        Pet pet = trackedPets.get(entity.getUniqueId());
        if (pet == null) {
            pet = repository.getByEntityUuid(entity.getUniqueId());
            if (pet != null) {
                trackedPets.put(entity.getUniqueId(), pet);
            }
        }

        if (pet == null) return;

        // Owner can always interact
        if (pet.getOwnerUuid().equals(player.getUniqueId().toString())) {
            return;
        }

        // Check if player has access
        if (!repository.isAllowed(pet, player.getUniqueId())) {
            // Only block if the pet is protected
            if (pet.isProtected()) {
                event.setCancelled(true);
                player.sendMessage(((RapunzelPaperCore) core).getMessageHandler().getMessage("pets.error.no_access"));
            }
        }
    }

    /**
     * Gets the pet repository.
     * @return the repository
     */
    public PetRepository getRepository() {
        return repository;
    }

    /**
     * Gets a pet by entity UUID.
     * @param entityUuid the entity UUID
     * @return the Pet, or null
     */
    public Pet getPet(UUID entityUuid) {
        Pet pet = trackedPets.get(entityUuid);
        if (pet == null) {
            pet = repository.getByEntityUuid(entityUuid);
            if (pet != null) {
                trackedPets.put(entityUuid, pet);
            }
        }
        return pet;
    }

    /**
     * Checks if an entity is a tracked pet.
     * @param entityUuid the entity UUID
     * @return true if it's a pet
     */
    public boolean isPet(UUID entityUuid) {
        return getPet(entityUuid) != null;
    }

    /**
     * Untracks a pet (when unclaimed).
     * @param entityUuid the entity UUID
     */
    public void untrackPet(UUID entityUuid) {
        trackedPets.remove(entityUuid);
    }

    /**
     * Tracks a pet.
     * @param pet the pet to track
     */
    public void trackPet(Pet pet) {
        if (pet != null && pet.getEntityUuid() != null) {
            trackedPets.put(UUID.fromString(pet.getEntityUuid()), pet);
        }
    }

    /**
     * Gets the ((RapunzelPaperCore) core) instance.
     * @return the ((RapunzelPaperCore) core)
     */
    public de.t14d3.rapunzelcore.RapunzelPaperCore getPlugin() {
        return ((RapunzelPaperCore) core);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
