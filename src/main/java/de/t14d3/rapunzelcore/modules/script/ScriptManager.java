package de.t14d3.rapunzelcore.modules.script;

import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.modules.Module;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScriptManager {
    private final Main plugin;
    private final Map<String, AliasData> aliases = new HashMap<>();
    private final Map<String, ScriptBlock> blocks = new HashMap<>();
    private Module module;
    private boolean hasVault = false;
    private Object economy;

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacyAmpersand = LegacyComponentSerializer.legacyAmpersand();
    private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

    public static class AliasData {
        public String script;
        public String permission;

        public AliasData(String script, String permission) {
            this.script = script;
            this.permission = permission;
        }
    }

    public ScriptManager(Main plugin, ScriptModule module) {
        this.plugin = plugin;
        this.module = module;
        detectVault();
        registerDefaultBlocks();
    }

    private void detectVault() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                hasVault = true;
                RegisteredServiceProvider<?> economyProvider = Bukkit.getServicesManager().getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"));
                if (economyProvider != null) {
                    economy = economyProvider.getProvider();
                    plugin.getLogger().info("Vault economy detected and loaded successfully");
                } else {
                    hasVault = false;
                }
            }
        } catch (Exception e) {
            hasVault = false;
        }
    }

    private void registerDefaultBlocks() {
        blocks.put("perm", new PermissionBlock());
        blocks.put("check", new CheckBlock());
        if (hasVault) {
            blocks.put("moneycost", new MoneyCostBlock());
            blocks.put("hasmoney", new HasMoneyBlock());
        }
        blocks.put("hasitem", new HasItemBlock());
        blocks.put("hasexp", new HasExpBlock());
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
                .replaceAll("<H>(.*?)</H>", "<hover:show_text:'$1'>")
                .replaceAll("<C>(.*?)</C>", "<click:run_command:'$1'>");
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
            executeLine(sender, line.trim());
        }
    }

    private void executeLine(CommandSender sender, String line) {
        if (line.isEmpty()) return;
        String[] parts = line.split("!");
        boolean conditionMet = true;
        for (String part : parts) {
            String[] split = part.split(":", 2);
            String name = split[0].trim();
            String args = split.length > 1 ? split[1].trim() : "";
            args = replacePlaceholders(args, sender);

            ScriptBlock block = blocks.get(name);
            if (block != null) {
                ScriptBlock.Result result = isConditionBlock(name) ?
                    (conditionMet ? block.execute(sender, args) : ScriptBlock.Result.SKIP) :
                    (conditionMet ? block.execute(sender, args) : ScriptBlock.Result.SKIP);
                conditionMet = (result == ScriptBlock.Result.SUCCESS);
                if (result == ScriptBlock.Result.FAIL) break;
            } else if (conditionMet) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacePlaceholders(part, sender));
            }
        }
    }

    private boolean isConditionBlock(String name) {
        return name.equals("perm") || name.equals("check") || name.equals("hasitem") || name.equals("hasexp") ||
               (hasVault && (name.equals("moneycost") || name.equals("hasmoney")));
    }

    private String replacePlaceholders(String text, CommandSender sender) {
        if (sender instanceof Player player) {
            return text.replace("<player>", player.getName())
                      .replace("[playerName]", player.getName())
                      .replace("<world>", player.getWorld().getName())
                      .replace("<x>", String.valueOf(player.getLocation().getBlockX()))
                      .replace("<y>", String.valueOf(player.getLocation().getBlockY()))
                      .replace("<z>", String.valueOf(player.getLocation().getBlockZ()))
                      .replace("<health>", String.valueOf(player.getHealth()))
                      .replace("<food>", String.valueOf(player.getFoodLevel()))
                      .replace("<level>", String.valueOf(player.getLevel()));
        }
        return text;
    }

    public boolean checkScriptPermissions(CommandSender sender, String script) {
        Set<String> usedBlocks = new HashSet<>();
        for (String line : script.split("\n")) {
            for (String part : line.split("!")) {
                String blockName = part.split(":", 2)[0].trim();
                if (blocks.containsKey(blockName)) usedBlocks.add(blockName);
            }
        }
        for (String block : usedBlocks) {
            if (sender instanceof Player player && !player.hasPermission("rapunzelcore.script.block." + block)) {
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
            creator.sendMessage(plugin.getMessageHandler().getMessage("script.permission.denied"));
            return;
        }
        script = convertLegacyToMiniMessage(script);
        aliases.put(name, new AliasData(script, null));
        registerAliasCommand(name);
    }

    private void registerAliasCommand(String name) {
        new CommandAPICommand(name)
                .withArguments(new GreedyStringArgument("args").setOptional(true))
                .executes((executor, args) -> {
                    AliasData data = aliases.get(name);
                    if (data == null) return 1;
                    if (data.permission != null && executor instanceof Player player && !player.hasPermission(data.permission)) {
                        executor.sendMessage(plugin.getMessageHandler().getMessage("alias.permission.denied"));
                        return 1;
                    }
                    String modifiedScript = injectArgs(data.script, ((String) args.getOrDefault("args", "")).split(" "));
                    executeScript(executor, modifiedScript);
                    return 1;
                })
                .register(plugin);
    }

    private String injectArgs(String script, String[] argParts) {
        String result = script;
        for (int i = 0; i < argParts.length; i++) {
            result = result.replace("$" + (i + 1), argParts[i]);
        }
        for (int i = 1; i <= 10; i++) {
            result = result.replace("$" + i, "");
        }
        return result;
    }

    public void removeAlias(String name) {
        aliases.remove(name);
        CommandAPI.unregister(name);
    }

    public Map<String, AliasData> getAliases() {
        return new HashMap<>(aliases);
    }

    public void editAliasLine(String name, int index, String newLine, CommandSender editor) {
        AliasData data = aliases.get(name);
        if (data != null) {
            String[] lines = data.script.split("\n");
            if (index >= 0 && index < lines.length) {
                newLine = convertLegacyToMiniMessage(newLine);
                lines[index] = newLine;
                String newScript = String.join("\n", lines);
                if (checkScriptPermissions(editor, newScript)) {
                    data.script = newScript;
                    editor.sendMessage(plugin.getMessageHandler().getMessage("alias.line.updated"));
                } else {
                    editor.sendMessage(plugin.getMessageHandler().getMessage("script.permission.denied"));
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
                editor.sendMessage(plugin.getMessageHandler().getMessage("alias.line.added"));
            } else {
                editor.sendMessage(plugin.getMessageHandler().getMessage("script.permission.denied"));
            }
        }
    }

    public void removeAliasLine(String name, int index) {
        AliasData data = aliases.get(name);
        if (data != null) {
            String[] lines = data.script.split("\n");
            if (index >= 0 && index < lines.length) {
                String[] newLines = new String[lines.length - 1];
                System.arraycopy(lines, 0, newLines, 0, index);
                System.arraycopy(lines, index + 1, newLines, index, lines.length - index - 1);
                data.script = String.join("\n", newLines);
            }
        }
    }

    public void loadAliases(FileConfiguration config) {
        for (String key : config.getKeys(false)) {
            if (config.isConfigurationSection(key)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section == null) continue;
                String script = section.getString("script");
                String permission = section.getString("permission");
                if (script == null) continue;
                script = convertLegacyToMiniMessage(script);
                aliases.put(key, new AliasData(script, permission));
                registerAliasCommand(key);
            } else {
                String script = config.getString(key);
                if (script == null) continue;
                script = convertLegacyToMiniMessage(script);
                aliases.put(key, new AliasData(script, null));
                registerAliasCommand(key);
            }
        }
    }



    public void saveAliases() {
        FileConfiguration config = ((ScriptModule) module).getConfig();
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }
        for (Map.Entry<String, AliasData> entry : aliases.entrySet()) {
            Map<String, Object> map = new HashMap<>();
            map.put("script", entry.getValue().script);
            map.put("permission", entry.getValue().permission);
            config.set(entry.getKey(), map);
        }
    }

    // ScriptBlock interface
    public interface ScriptBlock {
        enum Result { SUCCESS, FAIL, SKIP }
        Result execute(CommandSender sender, String args);
    }

    private class CheckBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (args.contains("==")) {
                String[] parts = args.split("==", 2);
                return parts[0].trim().equals(parts[1].trim()) ? Result.SUCCESS : Result.SKIP;
            } else if (args.contains("!=")) {
                String[] parts = args.split("!=", 2);
                return !parts[0].trim().equals(parts[1].trim()) ? Result.SUCCESS : Result.SKIP;
            }
            return Result.SKIP;
        }
    }

    private class PermissionBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            return (sender instanceof Player player && player.hasPermission(args)) ? Result.SUCCESS : Result.SKIP;
        }
    }

    private class MessageBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Component message = miniMessage.deserialize(processShorthandTags(args));
                player.sendMessage(message);
            }
            return Result.SUCCESS;
        }
    }

    private class BroadcastBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            Component message = miniMessage.deserialize(processShorthandTags(args));
            Bukkit.broadcast(message);
            return Result.SUCCESS;
        }
    }

    private class HealBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) player.setHealth(player.getMaxHealth());
            return Result.SUCCESS;
        }
    }

    private class FeedBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                player.setFoodLevel(20);
                player.setSaturation(20);
            }
            return Result.SUCCESS;
        }
    }

    private class AsConsoleBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), args);
            return Result.SUCCESS;
        }
    }

    private class AsPlayerBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            String[] split = args.split(":", 2);
            if (split.length >= 2) {
                Player target = Bukkit.getPlayer(split[0]);
                if (target != null) target.performCommand(split[1]);
            }
            return Result.SUCCESS;
        }
    }

    private class MoneyCostBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (!hasVault || economy == null || !(sender instanceof Player player)) return Result.FAIL;
            try {
                double amount = Double.parseDouble(args);
                Method hasMoney = economy.getClass().getMethod("has", Player.class, double.class);
                Method withdraw = economy.getClass().getMethod("withdrawPlayer", Player.class, double.class);
                if ((Boolean) hasMoney.invoke(economy, player, amount)) {
                    withdraw.invoke(economy, player, amount);
                    return Result.SUCCESS;
                }
                return Result.FAIL;
            } catch (Exception e) {
                return Result.FAIL;
            }
        }
    }

    private class HasMoneyBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (!hasVault || economy == null || !(sender instanceof Player player)) return Result.FAIL;
            try {
                double amount = Double.parseDouble(args);
                Method hasMoney = economy.getClass().getMethod("has", Player.class, double.class);
                return (Boolean) hasMoney.invoke(economy, player, amount) ? Result.SUCCESS : Result.FAIL;
            } catch (Exception e) {
                return Result.FAIL;
            }
        }
    }

    private class HasItemBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Material mat = Material.matchMaterial(args);
                return (mat != null && player.getInventory().contains(mat)) ? Result.SUCCESS : Result.FAIL;
            }
            return Result.SKIP;
        }
    }

    private class HasExpBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                int exp = Integer.parseInt(args);
                return player.getTotalExperience() >= exp ? Result.SUCCESS : Result.FAIL;
            }
            return Result.SKIP;
        }
    }

    private class GiveBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Material mat = Material.matchMaterial(args);
                if (mat != null) player.getInventory().addItem(new ItemStack(mat, 1));
            }
            return Result.SUCCESS;
        }
    }

    private class TakeBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                Material mat = Material.matchMaterial(args);
                if (mat != null) {
                    return player.getInventory().removeItem(new ItemStack(mat, 1)).isEmpty() ? Result.SUCCESS : Result.FAIL;
                }
            }
            return Result.SKIP;
        }
    }

    private class TeleportBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                String[] coords = args.split(",");
                Location loc;
                if (coords.length == 3) {
                    loc = new Location(player.getWorld(),
                            Double.parseDouble(coords[0]),
                            Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[2]));
                } else if (coords.length == 4) {
                    loc = new Location(Bukkit.getWorld(coords[0]),
                            Double.parseDouble(coords[1]),
                            Double.parseDouble(coords[2]),
                            Double.parseDouble(coords[3]));
                } else {
                    return Result.FAIL;
                }
                player.teleport(loc);
            }
            return Result.SUCCESS;
        }
    }

    private class SetExpBlock implements ScriptBlock {
        public Result execute(CommandSender sender, String args) {
            if (sender instanceof Player player) {
                player.setLevel(Integer.parseInt(args));
            }
            return Result.SUCCESS;
        }
    }

    }
