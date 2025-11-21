package de.t14d3.core;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

// Note: For economy blocks, Vault dependency is assumed
// import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScriptManager {
    private final Main plugin;
    private final Map<String, AliasData> aliases = new HashMap<>();
    private final Map<String, ScriptBlock> blocks = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacyAmpersand();
    private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

    public static class AliasData {
        public String script;
        public String permission; // permission required to see/use the alias, null for public

        public AliasData(String script, String permission) {
            this.script = script;
            this.permission = permission;
        }
    }

    public ScriptManager(Main plugin) {
        this.plugin = plugin;
        registerDefaultBlocks();
    }

    private void registerDefaultBlocks() {
        // Conditions
        blocks.put("perm", new PermissionBlock());
        blocks.put("check", new CheckBlock());
        blocks.put("moneycost", new MoneyCostBlock());
        blocks.put("hasmoney", new HasMoneyBlock());
        blocks.put("hasitem", new HasItemBlock());
        blocks.put("hasexp", new HasExpBlock());
        // Actions
        blocks.put("msg", new MessageBlock());
        blocks.put("broadcast", new BroadcastBlock());
        blocks.put("heal", new HealBlock());
        blocks.put("feed", new FeedBlock());
        blocks.put("asConsole", new AsConsoleBlock());
        blocks.put("asPlayer", new AsPlayerBlock());
        blocks.put("give", new GiveBlock());
        blocks.put("take", new TakeBlock());
        blocks.put("teleport", new TeleportBlock());
        blocks.put("setexp", new SetExpBlock());
    }

    private String processShorthandTags(String text) {
        return text
                // Replace <H>text</H> with <hover:show_text:'text'>
                .replaceAll("<H>(.*?)</H>", "<hover:show_text:'$1'>")
                // Replace <C >text< /> with <click:run_command:'text'>
                .replaceAll("<C>(.*?)</C>", "<click:run_command:'$1'>")
                ;
    }

    private String convertLegacyToMiniMessage(String text) {
        if (text.contains("&")) {
            Component component = legacyAmpersand.deserialize(text);
            return miniMessage.serialize(component);
        }
        if (text.contains("§")) {
            Component component = legacySection.deserialize(text);
            return miniMessage.serialize(component);
        }
        return text;
    }

    public void executeScript(CommandSender sender, String script) {
        String[] lines = script.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            executeLine(sender, line);
        }
    }

    private void executeLine(CommandSender sender, String line) {
        String[] parts = line.split("!");
        boolean conditionMet = true;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            String[] split = part.split(":", 2);
            String name = split[0].trim();
            String args = split.length > 1 ? split[1].trim() : "";
            args = replacePlaceholders(args, sender);

            ScriptBlock block = blocks.get(name);
            if (block != null) {
                ScriptBlock.Result result;
                if (isConditionBlock(name)) {
                    result = block.execute(sender, args);
                    conditionMet = (result == ScriptBlock.Result.SUCCESS);
                } else {
                    if (conditionMet) {
                        result = block.execute(sender, args);
                    } else {
                        result = ScriptBlock.Result.SKIP;
                    }
                }
                if (result == ScriptBlock.Result.FAIL) {
                    break;
                }
                // For SKIP, continue
            } else {
                // Assume it's a command
                String command = replacePlaceholders(part, sender);
                if (conditionMet) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
        }
    }

    private boolean isConditionBlock(String name) {
        return name.equals("perm") || name.equals("check") || name.equals("moneycost") || name.equals("hasmoney") || name.equals("hasitem") || name.equals("hasexp");
    }

    private String replacePlaceholders(String text, CommandSender sender) {
        if (sender instanceof Player player) {
            text = text.replace("<player>", player.getName());
            text = text.replace("[playerName]", player.getName()); // legacy
            text = text.replace("<world>", player.getWorld().getName());
            text = text.replace("<x>", String.valueOf(player.getLocation().getBlockX()));
            text = text.replace("<y>", String.valueOf(player.getLocation().getBlockY()));
            text = text.replace("<z>", String.valueOf(player.getLocation().getBlockZ()));
            text = text.replace("<health>", String.valueOf(player.getHealth()));
            text = text.replace("<food>", String.valueOf(player.getFoodLevel()));
            text = text.replace("<level>", String.valueOf(player.getLevel()));
            // Add more as needed
        }
        return text;
    }

    public boolean checkScriptPermissions(CommandSender sender, String script) {
        Set<String> usedBlocks = new HashSet<>();
        String[] lines = script.split("\n");
        for (String line : lines) {
            String[] parts = line.split("!");
            for (String part : parts) {
                String[] split = part.split(":", 2);
                String blockName = split[0].trim();
                if (!usedBlocks.contains(blockName) && blocks.containsKey(blockName)) {
                    usedBlocks.add(blockName);
                }
            }
        }
        for (String block : usedBlocks) {
            String perm = "core.script.block." + block;
            if (sender instanceof Player player && !player.hasPermission(perm)) {
                return false;
            }
        }
        return true;
    }

    public void addAlias(String name, String script) {
        addAlias(name, script, null);
    }

    public void addAlias(String name, String script, CommandSender creator) {
        if (creator != null && !checkScriptPermissions(creator, script)) {
            creator.sendMessage(plugin.getMessage("script.permission.denied"));
            return;
        }
        script = convertLegacyToMiniMessage(script);
        aliases.put(name, new AliasData(script, null));
        // Register dynamic command with args
        new CommandAPICommand(name)
                .withArguments(new GreedyStringArgument("args").setOptional(true))
                .executes((executor, args) -> {
                    AliasData data = aliases.get(name);
                    if (data != null) {
                        if (data.permission != null && executor instanceof Player player && !player.hasPermission(data.permission)) {
                            executor.sendMessage(plugin.getMessage("alias.permission.denied"));
                            return 1;
                        }
                        String currentScript = data.script;
                        String argsStr = (String) args.getOrDefault("args", "");
                        String[] argParts = argsStr.isEmpty() ? new String[0] : argsStr.split(" ");
                        String modifiedScript = injectArgs(currentScript, argParts);
                        executeScript(executor, modifiedScript);
                    }
                    return 1;
                })
                .register(plugin);
        saveAliases();
    }

    private String injectArgs(String script, String[] argParts) {
        String result = script;
        for (int i = 0; i < argParts.length; i++) {
            result = result.replace("$" + (i + 1), argParts[i]);
        }
        // Replace remaining $n with empty
        for (int i = 1; i <= 10; i++) { // arbitrary max
            result = result.replace("$" + i, "");
        }
        return result;
    }

    public void removeAlias(String name) {
        aliases.remove(name);
        // Note: CommandAPI doesn't easily allow unregistering, so perhaps reload or ignore
        saveAliases();
    }

    public Map<String, AliasData> getAliases() {
        return new HashMap<>(aliases);
    }

    public void editAliasLine(String name, int index, String newLine, CommandSender editor) {
        AliasData data = aliases.get(name);
        if (data != null) {
            String script = data.script;
            String[] lines = script.split("\n");
            if (index >= 0 && index < lines.length) {
                newLine = convertLegacyToMiniMessage(newLine);
                lines[index] = newLine;
                String newScript = String.join("\n", lines);
                if (checkScriptPermissions(editor, newScript)) {
                    data.script = newScript;
                    saveAliases();
                    editor.sendMessage(plugin.getMessage("alias.line.updated"));
                } else {
                    editor.sendMessage(plugin.getMessage("script.permission.denied"));
                }
            }
        }
    }

    public void addAliasLine(String name, String newLine, CommandSender editor) {
        AliasData data = aliases.get(name);
        if (data != null) {
            newLine = convertLegacyToMiniMessage(newLine);
            String newScript = data.script + "\n" + newLine;
            if (checkScriptPermissions(editor, newScript)) {
                data.script = newScript;
                saveAliases();
                editor.sendMessage(plugin.getMessage("alias.line.added"));
            } else {
                editor.sendMessage(plugin.getMessage("script.permission.denied"));
            }
        }
    }

    public void removeAliasLine(String name, int index) {
        AliasData data = aliases.get(name);
        if (data != null) {
            String script = data.script;
            String[] lines = script.split("\n");
            if (index >= 0 && index < lines.length) {
                String[] newLines = new String[lines.length - 1];
                System.arraycopy(lines, 0, newLines, 0, index);
                System.arraycopy(lines, index + 1, newLines, index, lines.length - index - 1);
                String newScript = String.join("\n", newLines);
                data.script = newScript;
                saveAliases();
                // No permission check for removal, assume allowed if they can edit
            }
        }
    }

    public void loadAliases() {
        File aliasesFile = new File(plugin.getDataFolder(), "aliases.yml");
        if (!aliasesFile.exists()) {
            try {
                aliasesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create aliases.yml: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(aliasesFile);

        for (String key : config.getKeys(false)) {
            // Prefer treating the key as a section if present
            if (config.isConfigurationSection(key)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String script = section.getString("script", null);
                String permission = section.getString("permission", null);

                if (script == null) {
                    continue;
                }

                script = convertLegacyToMiniMessage(script);

                aliases.put(key, new AliasData(script, permission));
                addAlias(key, script); // register command
            } else {
                // Backward compatibility: scalar value (string)
                // Use getString to avoid direct casting
                String script = config.getString(key, null);
                if (script == null) {
                    continue;
                }

                script = convertLegacyToMiniMessage(script);

                aliases.put(key, new AliasData(script, null));
                addAlias(key, script); // register command
            }
        }
    }

    public void saveAliases() {
        File aliasesFile = new File(plugin.getDataFolder(), "aliases.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(aliasesFile);
        for (Map.Entry<String, AliasData> entry : aliases.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("script", entry.getValue().script);
            map.put("permission", entry.getValue().permission);
            config.set(entry.getKey(), map);
        }
        try {
            config.save(aliasesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save aliases.yml: " + e.getMessage());
        }
    }

    // ScriptBlock interface
    public interface ScriptBlock {
        enum Result {
            SUCCESS, // continue
            FAIL, // stop execution
            SKIP // skip but continue
        }

        Result execute(CommandSender sender, String args);
    }

    private class CheckBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            // args like $1==track or $2!=null
            if (args.contains("==")) {
                String[] parts = args.split("==", 2);
                String left = parts[0].trim();
                String right = parts[1].trim();
                if (left.equals(right)) {
                    return Result.SUCCESS;
                }
            } else if (args.contains("!=")) {
                String[] parts = args.split("!=", 2);
                String left = parts[0].trim();
                String right = parts[1].trim();
                if (!left.equals(right)) {
                    return Result.SUCCESS;
                }
            }
            return Result.SKIP;
        }
    }

    // Default blocks
    private class PermissionBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (!(sender instanceof Player player) || !player.hasPermission(args)) {
                return Result.SKIP;
            }
            return Result.SUCCESS;
        }
    }

    private class MessageBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                // Process shorthand tags and parse MiniMessage
                String processed = processShorthandTags(args);
                Component message = miniMessage.deserialize(processed);
                player.sendMessage(message);
            }
            return Result.SUCCESS;
        }
    }

    private class BroadcastBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            // Process shorthand tags and parse MiniMessage
            String processed = processShorthandTags(args);
            Component message = miniMessage.deserialize(processed);
            Bukkit.broadcast(message);
            return Result.SUCCESS;
        }
    }

    private class HealBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                player.setHealth(player.getMaxHealth());
            }
            return Result.SUCCESS;
        }
    }

    private class FeedBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                player.setFoodLevel(20);
                player.setSaturation(20);
            }
            return Result.SUCCESS;
        }
    }

    private class AsConsoleBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), args);
            return Result.SUCCESS;
        }
    }

    private class AsPlayerBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            // args: playerName:command
            String[] split = args.split(":", 2);
            if (split.length >= 2) {
                Player target = Bukkit.getPlayer(split[0]);
                if (target != null) {
                    target.performCommand(split[1]);
                }
            }
            return Result.SUCCESS;
        }
    }

    // New blocks
    private class MoneyCostBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            // Note: Requires Vault economy
            // if (sender instanceof Player player) {
            //     double amount = Double.parseDouble(args);
            //     Economy econ = ... get economy
            //     if (econ.has(player, amount)) {
            //         econ.withdrawPlayer(player, amount);
            //         return Result.SUCCESS;
            //     }
            // }
            // return Result.FAIL;
            // For now, placeholder
            return Result.SUCCESS; // Assume success
        }
    }

    private class HasMoneyBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            // Similar to above, check without withdrawing
            return Result.SUCCESS;
        }
    }

    private class HasItemBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                // Simple check for material
                Material mat = Material.matchMaterial(args);
                if (mat != null && player.getInventory().contains(mat)) {
                    return Result.SUCCESS;
                }
            }
            return Result.SKIP;
        }
    }

    private class HasExpBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                int exp = Integer.parseInt(args);
                if (player.getTotalExperience() >= exp) {
                    return Result.SUCCESS;
                }
            }
            return Result.SKIP;
        }
    }

    private class GiveBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Material mat = Material.matchMaterial(args);
                if (mat != null) {
                    player.getInventory().addItem(new ItemStack(mat, 1));
                }
            }
            return Result.SUCCESS;
        }
    }

    private class TakeBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Material mat = Material.matchMaterial(args);
                if (mat != null) {
                    player.getInventory().removeItem(new ItemStack(mat, 1));
                }
            }
            return Result.SUCCESS;
        }
    }

    private class TeleportBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                // args: world,x,y,z or x,y,z (current world)
                String[] coords = args.split(",");
                if (coords.length == 3) {
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    double z = Double.parseDouble(coords[2]);
                    Location loc = new Location(player.getWorld(), x, y, z);
                    player.teleport(loc);
                } else if (coords.length == 4) {
                    String world = coords[0];
                    double x = Double.parseDouble(coords[1]);
                    double y = Double.parseDouble(coords[2]);
                    double z = Double.parseDouble(coords[3]);
                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                    player.teleport(loc);
                }
            }
            return Result.SUCCESS;
        }
    }

    private class SetExpBlock implements ScriptBlock {
        @Override
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                int exp = Integer.parseInt(args);
                player.setLevel(exp);
            }
            return Result.SUCCESS;
        }
    }
}
