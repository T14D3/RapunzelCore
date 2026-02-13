package de.t14d3.rapunzelcore.modules.pets;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.modules.commands.Command;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.List;

/**
 * Main /pet command with subcommands.
 */
public class PetCommand implements Command {

    private final RapunzelPaperCore core;
    private final PetModule module;
    private final PetRepository repository;

    // Subcommands
    private final PetInfoCommand infoCommand;
    private final PetUnclaimCommand unclaimCommand;
    private final PetAccessCommand accessCommand;
    private final PetDenyCommand denyCommand;

    public PetCommand(RapunzelPaperCore core, PetModule module) {
        this.core = core;
        this.module = module;
        this.repository = module.getRepository();
        
        // Initialize subcommands
        this.infoCommand = new PetInfoCommand(core, module);
        this.unclaimCommand = new PetUnclaimCommand(core, module);
        this.accessCommand = new PetAccessCommand(core, module);
        this.denyCommand = new PetDenyCommand(core, module);
        
        register();
    }

    @Override
    public void register() {
        // Main /pet command - shows info when no subcommand
        new CommandAPICommand("pet")
                .withFullDescription("Pet management commands")
                .withPermission("rapunzelcore.pet")
                .executesPlayer((player, args) -> {
                    // Default behavior: show pet info for nearby pet
                    infoCommand.showNearestPetInfo(player);
                    return Command.SINGLE_SUCCESS;
                })
                .register(core);

        // /pet info [name] - View pet information
        new CommandAPICommand("pet")
                .withSubcommand(new CommandAPICommand("info")
                        .withOptionalArguments(new StringArgument("name"))
                        .withFullDescription("View pet information")
                        .withPermission("rapunzelcore.pet")
                        .executesPlayer((player, args) -> {
                            String name = (String) args.getOptional("name").orElse(null);
                            if (name != null) {
                                infoCommand.showPetInfoByName(player, name);
                            } else {
                                infoCommand.showNearestPetInfo(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(core);

        // /pet unclaim - Release pet
        new CommandAPICommand("pet")
                .withSubcommand(new CommandAPICommand("unclaim")
                        .withOptionalArguments(new StringArgument("name"))
                        .withFullDescription("Release your pet")
                        .withPermission("rapunzelcore.pet.unclaim")
                        .executesPlayer((player, args) -> {
                            String name = (String) args.getOptional("name").orElse(null);
                            unclaimCommand.unclaimPet(player, name);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(core);

        // /pet access <player> [pet_name] - Allow other players to interact
        new CommandAPICommand("pet")
                .withSubcommand(new CommandAPICommand("access")
                        .withArguments(new StringArgument("player"))
                        .withOptionalArguments(new StringArgument("pet_name"))
                        .withFullDescription("Allow a player to interact with your pet")
                        .withPermission("rapunzelcore.pet.access")
                        .executesPlayer((player, args) -> {
                            String targetPlayer = (String) args.get("player");
                            String petName = (String) args.getOptional("pet_name").orElse(null);
                            accessCommand.grantAccess(player, targetPlayer, petName);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(core);

        // /pet deny <player> [pet_name] - Remove pet access
        new CommandAPICommand("pet")
                .withSubcommand(new CommandAPICommand("deny")
                        .withArguments(new StringArgument("player"))
                        .withOptionalArguments(new StringArgument("pet_name"))
                        .withFullDescription("Remove a player's access to your pet")
                        .withPermission("rapunzelcore.pet.deny")
                        .executesPlayer((player, args) -> {
                            String targetPlayer = (String) args.get("player");
                            String petName = (String) args.getOptional("pet_name").orElse(null);
                            denyCommand.revokeAccess(player, targetPlayer, petName);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(core);

        // /pet list - List all your pets
        new CommandAPICommand("pet")
                .withSubcommand(new CommandAPICommand("list")
                        .withFullDescription("List all your pets")
                        .withPermission("rapunzelcore.pet")
                        .executesPlayer((player, args) -> {
                            infoCommand.listPets(player);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .register(core);
    }

    @Override
    public void unregister() {
        List.of("pet").forEach(CommandAPI::unregister);
    }
}
