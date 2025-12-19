package de.t14d3.rapunzelcore.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.authlib.GameProfile;
import de.t14d3.rapunzelcore.Main;
import de.t14d3.rapunzelcore.MessageHandler;
import de.t14d3.rapunzelcore.modules.chat.ChatModule;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.object.ObjectContents;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public class Utils {
    private Utils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void setOfflineLocation(OfflinePlayer player, Location location) {
        setOfflineLocation(player.getPlayerProfile(), location);
    }

    public static void setOfflineLocation(PlayerProfile originalPlayerProfile, Location originalLocation) {
        PlayerProfile playerProfile = originalPlayerProfile.clone();
        Location location = originalLocation.clone();
        new Thread(null, () -> {
            playerProfile.complete(false);
            if (playerProfile.getId() == null || playerProfile.getName() == null) {
                throw new IllegalArgumentException("PlayerProfile must have an ID and name set");
            }
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            GameProfile profile = new GameProfile(playerProfile.getId(), playerProfile.getName());
            NameAndId nameAndId = new NameAndId(profile);
            CompoundTag playerData = server.playerDataStorage.load(nameAndId).orElse(null);
            if (playerData == null) {
                return;
            }
            ListTag locationTag = (ListTag) playerData.get("Pos");
            if (locationTag == null) {
                return;
            }
            locationTag.set(0, DoubleTag.valueOf(location.getX()));
            locationTag.set(1, DoubleTag.valueOf(location.getY()));
            locationTag.set(2, DoubleTag.valueOf(location.getZ()));
            playerData.put("Pos", locationTag);

            Level level = ((CraftWorld) location.getWorld()).getHandle();
            StringTag dimensionTag = StringTag.valueOf(level.dimension().location().toString());
            playerData.put("Dimension", dimensionTag);

            Path filePath = server.playerDataStorage.getPlayerDir().toPath().resolve(nameAndId.id() + ".dat");
            try {
                NbtIo.writeCompressed(playerData, filePath);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("Failed to save player data for " + nameAndId.name() + " to " + filePath);
            }
        }, "RapunzelCore-PlayerDataSaver").start();
    }

    @SuppressWarnings("PatternValidation")
    public static TagResolver itemResolver(Player player) {
        return TagResolver.resolver("item", (args, context) -> {
            ItemStack item = player.getInventory().getItemInMainHand();
            String[] iconConfig = ChatModule.getIconConfig();
            Key atlas = Key.key(iconConfig[0]);
            Key icon = Key.key(iconConfig[1]);
            String format = Main.getInstance().getMessageHandler().getRaw("general.component.item");
            Component name = Component.translatable(item.getType().translationKey());
            String amount = item.getAmount() + "";
            Component result = MiniMessage.miniMessage().deserialize(format,
                    Placeholder.component("icon", Component.object(ObjectContents.sprite(atlas, icon))),
                    Placeholder.component("name", name),
                    Placeholder.parsed("amount", amount),
                    StandardTags.defaults()
            );
            return Tag.selfClosingInserting(result.hoverEvent(item.asHoverEvent()));
        });
    }
}
