package de.t14d3.rapunzelcore.modules.commands;

import de.t14d3.rapunzelcore.RapunzelCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeCommand implements Command {

    public void register() {
        new CommandAPICommand("recipe")
                .withFullDescription("Shows the recipe for a specific item.")
                .withPermission("rapunzelcore.commands.recipe")
                .withArguments(new StringArgument("material"))
                .executesPlayer((player, args) -> {
                    Material material = Material.matchMaterial((String) args.get("material"));
                    showRecipe(player, material);
                    return Command.SINGLE_SUCCESS;
                })
                .register((JavaPlugin) RapunzelCore.getInstance());
    }

    private void showRecipe(Player player, Material material) {
        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(material));

        if (recipes.isEmpty()) {
            player.sendMessage(RapunzelCore.getInstance().getMessageHandler().getMessage("commands.recipe.error.no_recipe", material.name().toLowerCase()));
            return;
        }

        // Show the first recipe
        Recipe recipe = recipes.get(0);
        sendRecipeInfo(player, recipe, material);
    }

    private void sendRecipeInfo(Player player, Recipe recipe, Material result) {
        MiniMessage mm = MiniMessage.miniMessage();
        
        // Header
        player.sendMessage(mm.deserialize("<gold>=== Recipe: <yellow>" + result.name() + " <gold>==="));
        
        if (recipe instanceof ShapedRecipe shaped) {
            sendShapedRecipe(player, shaped);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            sendShapelessRecipe(player, shapeless);
        } else if (recipe instanceof FurnaceRecipe furnace) {
            sendFurnaceRecipe(player, furnace);
        } else if (recipe instanceof SmithingRecipe smithing) {
            sendSmithingRecipe(player, smithing);
        } else {
            player.sendMessage(mm.deserialize("<red>Unknown recipe type: " + recipe.getClass().getSimpleName()));
        }
        
        // Result
        player.sendMessage(mm.deserialize("<green>Result: <aqua>" + result.name()));
    }

    private void sendShapedRecipe(Player player, ShapedRecipe recipe) {
        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(mm.deserialize("<blue>Type: <aqua>Shaped Recipe (Crafting Table)"));
        player.sendMessage(mm.deserialize("<gray>Shape:"));
        
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredientMap = recipe.getIngredientMap();
        
        for (String row : shape) {
            StringBuilder rowMsg = new StringBuilder("<gray>  ");
            for (char c : row.toCharArray()) {
                ItemStack ingredient = ingredientMap.get(c);
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    rowMsg.append("[<aqua>").append(ingredient.getType().name()).append("<gray>] ");
                } else {
                    rowMsg.append("[<dark_gray>empty<gray>] ");
                }
            }
            player.sendMessage(mm.deserialize(rowMsg.toString()));
        }
    }

    private void sendShapelessRecipe(Player player, ShapelessRecipe recipe) {
        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(mm.deserialize("<blue>Type: <aqua>Shapeless Recipe (Crafting Table)"));
        player.sendMessage(mm.deserialize("<gray>Ingredients:"));
        
        List<String> ingredients = new ArrayList<>();
        for (RecipeChoice choice : recipe.getChoiceList()) {
            String ingredientName = getChoiceDisplayName(choice);
            ingredients.add(ingredientName);
        }
        
        for (String ingredient : ingredients) {
            player.sendMessage(mm.deserialize("<gray>  - <aqua>" + ingredient));
        }
    }

    private void sendFurnaceRecipe(Player player, FurnaceRecipe recipe) {
        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(mm.deserialize("<blue>Type: <aqua>Furnace Recipe"));
        player.sendMessage(mm.deserialize("<gray>Input: <aqua>" + getChoiceDisplayName(recipe.getInputChoice())));
        player.sendMessage(mm.deserialize("<gray>Cooking Time: <aqua>" + (recipe.getCookingTime() / 20.0) + " seconds"));
        player.sendMessage(mm.deserialize("<gray>Experience: <aqua>" + recipe.getExperience()));
    }

    private void sendSmithingRecipe(Player player, SmithingRecipe recipe) {
        MiniMessage mm = MiniMessage.miniMessage();
        player.sendMessage(mm.deserialize("<blue>Type: <aqua>Smithing Recipe (Smithing Table)"));
        
        if (recipe instanceof SmithingTransformRecipe transform) {
            player.sendMessage(mm.deserialize("<gray>Template: <aqua>" + getChoiceDisplayName(transform.getTemplate())));
            player.sendMessage(mm.deserialize("<gray>Base Item: <aqua>" + getChoiceDisplayName(transform.getBase())));
            player.sendMessage(mm.deserialize("<gray>Addition: <aqua>" + getChoiceDisplayName(transform.getAddition())));
        } else if (recipe instanceof SmithingTrimRecipe trim) {
            player.sendMessage(mm.deserialize("<gray>Template: <aqua>" + getChoiceDisplayName(trim.getTemplate())));
            player.sendMessage(mm.deserialize("<gray>Base Item: <aqua>" + getChoiceDisplayName(trim.getBase())));
            player.sendMessage(mm.deserialize("<gray>Material: <aqua>" + getChoiceDisplayName(trim.getAddition())));
        }
    }

    private String getChoiceDisplayName(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            List<Material> choices = materialChoice.getChoices();
            if (!choices.isEmpty()) {
                if (choices.size() == 1) {
                    return choices.get(0).name();
                } else {
                    return choices.get(0).name() + " (or " + (choices.size() - 1) + " other options)";
                }
            }
        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            List<ItemStack> choices = exactChoice.getChoices();
            if (!choices.isEmpty()) {
                return choices.get(0).getType().name() + " (exact)";
            }
        }
        return "UNKNOWN";
    }
}
