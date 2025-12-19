package de.t14d3.rapunzelcore.util;

import com.google.gson.Gson;
import de.t14d3.rapunzelcore.Main;
import io.papermc.paper.connection.PlayerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public class Messenger implements PluginMessageListener {
    private final String channel = "rapunzelcore";
    private static final Messenger instance = new Messenger();

    private static final GsonComponentSerializer serializer = GsonComponentSerializer.gson();

    public Messenger() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(Main.getInstance(), channel);
        Bukkit.getMessenger().registerIncomingPluginChannel(Main.getInstance(), channel, this);
    }


    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(this.channel)) return;

    }

    public static Messenger getInstance() {
        return instance;
    }

    public void sendMessage(Player player, Component message) {
        player.sendPluginMessage(Main.getInstance(), channel, toBytes(message));
    }

    public static Component fromBytes(byte[] bytes) {
        try {
            // Read first two bytes
            int length = bytes[0] << 8 | bytes[1];
            // Read component from stream
            String json = new String(bytes, 2, length, StandardCharsets.UTF_8);
            return serializer.deserialize(json);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Failed to deserialize component: " + e.getMessage());
        }

        return Component.empty();
    }

    public static byte[] toBytes(Component component) {

        String json = serializer.serialize(component);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            bos.write(0);
            bos.write(0);
            // Write component to stream
            bos.write(json.getBytes());

            return bos.toByteArray();
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Failed to serialize component: " + e.getMessage());
        }

        return new byte[0];
    }
}
