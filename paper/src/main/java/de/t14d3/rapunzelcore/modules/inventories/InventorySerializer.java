package de.t14d3.rapunzelcore.modules.inventories;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class InventorySerializer {
    private static final Gson GSON = new Gson();
    private static final Type EFFECT_LIST_TYPE = new TypeToken<List<PotionEffectSnapshot>>() {}.getType();

    private final Logger logger;

    InventorySerializer(Logger logger) {
        this.logger = logger;
    }

    String encodeItems(ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
                 BukkitObjectOutputStream oos = new BukkitObjectOutputStream(gzip)) {
                oos.writeInt(items.length);
                for (ItemStack item : items) {
                    oos.writeObject(item);
                }
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.warn("[Inventories] Failed to encode items: {}", e.getMessage());
            return "";
        }
    }

    ItemStack[] decodeItems(String data) {
        if (data == null || data.isBlank()) return new ItemStack[0];
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(gzip)) {
                int length = ois.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    Object obj = ois.readObject();
                    if (obj instanceof ItemStack stack) {
                        items[i] = stack;
                    }
                }
                return items;
            }
        } catch (Exception e) {
            logger.warn("[Inventories] Failed to decode items: {}", e.getMessage());
            return new ItemStack[0];
        }
    }

    String encodeEffects(Collection<PotionEffect> effects) {
        if (effects == null || effects.isEmpty()) return "";
        List<PotionEffectSnapshot> snapshot = effects.stream()
            .filter(Objects::nonNull)
            .map(PotionEffectSnapshot::from)
            .filter(Objects::nonNull)
            .toList();
        if (snapshot.isEmpty()) return "";
        try {
            return GSON.toJson(snapshot);
        } catch (Exception e) {
            logger.warn("[Inventories] Failed to encode potion effects: {}", e.getMessage());
            return "";
        }
    }

    List<PotionEffect> decodeEffects(String data) {
        if (data == null || data.isBlank()) return List.of();
        try {
            List<PotionEffectSnapshot> snapshot = GSON.fromJson(data, EFFECT_LIST_TYPE);
            if (snapshot == null || snapshot.isEmpty()) return List.of();
            return snapshot.stream()
                .map(PotionEffectSnapshot::toEffect)
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            logger.warn("[Inventories] Failed to decode potion effects: {}", e.getMessage());
            return List.of();
        }
    }

    private record PotionEffectSnapshot(
        String type,
        int duration,
        int amplifier,
        boolean ambient,
        boolean particles,
        boolean icon
    ) {
        static PotionEffectSnapshot from(PotionEffect effect) {
            if (effect == null || effect.getType() == null || effect.getType().getKey() == null) return null;
            return new PotionEffectSnapshot(
                effect.getType().getKey().asString(),
                effect.getDuration(),
                effect.getAmplifier(),
                effect.isAmbient(),
                effect.hasParticles(),
                effect.hasIcon()
            );
        }

        PotionEffect toEffect() {
            if (type == null || type.isBlank()) return null;
            NamespacedKey key = NamespacedKey.fromString(type);
            PotionEffectType effectType = key == null ? null : PotionEffectType.getByKey(key);
            if (effectType == null) return null;
            return new PotionEffect(effectType, duration, amplifier, ambient, particles, icon);
        }
    }
}
