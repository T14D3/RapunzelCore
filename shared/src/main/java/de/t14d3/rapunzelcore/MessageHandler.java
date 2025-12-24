package de.t14d3.rapunzelcore;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import net.kyori.adventure.text.Component;

import java.util.Objects;
import java.util.function.Consumer;

public class MessageHandler {

    public MessageHandler() {
        reloadMessages();
    }

    public void reloadMessages() {
        messages().reload();
    }

    public Component getMessage(String key) {
        return messages().component(key);
    }

    public Component getMessage(String key, Placeholders placeholders) {
        Objects.requireNonNull(placeholders, "placeholders");
        return messages().component(key, placeholders);
    }

    public Component getMessage(String key, Consumer<Placeholders.Builder> builder) {
        Objects.requireNonNull(builder, "builder");
        Placeholders.Builder ph = Placeholders.builder();
        builder.accept(ph);
        return getMessage(key, ph.build());
    }

    public Component getMessage(String key, String arg1) {
        return getMessage(key, new String[]{arg1});
    }

    public Component getMessage(String key, String... args) {
        return getMessage(key, ph -> {
            for (int i = 0; i < args.length; i++) {
                ph.string("arg" + (i + 1), args[i]);
            }
        });
    }

    public Component getMessage(String key, Component... args) {
        return getMessage(key, ph -> {
            for (int i = 0; i < args.length; i++) {
                ph.component("arg" + (i + 1), args[i]);
            }
        });
    }

    public String getRaw(String key) {
        return messages().raw(key);
    }

    private static MessageFormatService messages() {
        return Rapunzel.context().messages();
    }
}

