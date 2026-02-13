package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.entities.Pet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to view pet information.
 */
public class PetInfoCommand {

    private static final int NEARBY_RADIUS = 5;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final RapunzelPaperCore core;
    private final PetModule module;
    private final PetRepository repository;

    public PetInfoCommand(RapunzelPaperCore core, PetModule module) {
        this.core = core;
        this.module = module;
        this.repository = module.getRepository();
        dateFormat.setTimeZone(TimeZone.getDefault());
    }

    /**
     * Shows information about the nearest pet to the player.
     */
    public void showNearestPetInfo(Player player) {
        Pet nearestPet = findNearestPet(player);
        if (nearestPet == null) {
            player.sendMessage(core.getMessageHandler().getMessage("pets.error.no_pet_nearby"));
            return;
        }
        showPetInfo(player, nearestPet);
    }

    /**
     * Shows information about a pet by name.
     */
    public void showPetInfoByName(Player player, String name) {
        // First check if player owns a pet with this name
        Pet pet = repository.getByOwnerAndName(player.getUniqueId(), name);
        
        if (pet == null) {
            // Check nearby pets with this name
            pet = findNearestPetByName(player, name);
        }
        
        if (pet == null) {
            player.sendMessage(core.getMessageHandler().getMessage("pets.error.not_found", name));
            return;
        }
        
        showPetInfo(player, pet);
    }

    /**
     * Lists all pets owned by the player.
     */
    public void listPets(Player player) {
        List<Pet> pets = repository.getByOwner(player.getUniqueId());
        
        if (pets.isEmpty()) {
            player.sendMessage(core.getMessageHandler().getMessage("pets.list.empty"));
            return;
        }

        player.sendMessage(core.getMessageHandler().getMessage("pets.list.header", String.valueOf(pets.size())));
        
        for (Pet pet : pets) {
            String name = pet.getCustomName() != null ? pet.getCustomName() : pet.getEntityType();
            String location = pet.getLastServer() != null ? pet.getLastServer() : "Unknown";
            
            Component line = miniMessage.deserialize(
                "<gray>- <white>" + name + " <gray>(<yellow>" + pet.getEntityType() + "<gray>) on <aqua>" + location
            );
            player.sendMessage(line);
        }
    }

    /**
     * Shows detailed information about a pet.
     */
    public void showPetInfo(Player player, Pet pet) {
        boolean isOwner = pet.getOwnerUuid().equals(player.getUniqueId().toString());
        boolean hasAccess = repository.isAllowed(pet, player.getUniqueId());
        
        // Header with pet name
        Component header = miniMessage.deserialize(
            "<gold><st>          </st> <yellow>Pet Info <gold><st>          </st>"
        );
        player.sendMessage(header);
        
        // Basic info
        String displayName = pet.getCustomName() != null ? pet.getCustomName() : "Unnamed";
        player.sendMessage(miniMessage.deserialize("<gray>Name: <white>" + displayName));
        player.sendMessage(miniMessage.deserialize("<gray>Type: <white>" + formatEntityType(pet.getEntityType())));
        
        // Owner info
        UUID ownerUuid = UUID.fromString(pet.getOwnerUuid());
        String ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
        if (ownerName == null) ownerName = "Unknown";
        String ownerStatus = isOwner ? " <green>(You)" : "";
        player.sendMessage(miniMessage.deserialize("<gray>Owner: <white>" + ownerName + ownerStatus));
        
        // Protection status
        String protection = pet.isProtected() ? "<green>Enabled" : "<red>Disabled";
        player.sendMessage(miniMessage.deserialize("<gray>Protection: " + protection));
        
        // Location info
        String server = pet.getLastServer() != null ? pet.getLastServer() : "Unknown";
        player.sendMessage(miniMessage.deserialize("<gray>Last Server: <aqua>" + server));
        player.sendMessage(miniMessage.deserialize("<gray>Last Seen: <white>" + dateFormat.format(new Date(pet.getLastSeenAt()))));
        
        // Access info (only for owner)
        if (isOwner) {
            Set<UUID> allowedPlayers = repository.getAllowedPlayers(pet);
            if (!allowedPlayers.isEmpty()) {
                String allowedNames = allowedPlayers.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
                player.sendMessage(miniMessage.deserialize("<gray>Allowed Players: <white>" + allowedNames));
            }
        } else if (hasAccess) {
            player.sendMessage(miniMessage.deserialize("<gray>Access: <green>You have access to this pet"));
        }
    }

    /**
     * Finds the nearest pet to a player.
     */
    private Pet findNearestPet(Player player) {
        double nearestDistance = Double.MAX_VALUE;
        Pet nearestPet = null;

        for (Entity entity : player.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)) {
            Pet pet = module.getPet(entity.getUniqueId());
            if (pet != null) {
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
     * Finds the nearest pet with a matching name.
     */
    private Pet findNearestPetByName(Player player, String name) {
        String searchName = name.toLowerCase();
        
        for (Entity entity : player.getNearbyEntities(NEARBY_RADIUS, NEARBY_RADIUS, NEARBY_RADIUS)) {
            Pet pet = module.getPet(entity.getUniqueId());
            if (pet != null) {
                String petName = pet.getCustomName() != null ? pet.getCustomName().toLowerCase() : pet.getEntityType().toLowerCase();
                if (petName.contains(searchName)) {
                    return pet;
                }
            }
        }
        
        return null;
    }

    private String formatEntityType(String type) {
        return Arrays.stream(type.toLowerCase().split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }
}
