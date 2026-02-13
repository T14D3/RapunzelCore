package de.t14d3.rapunzelcore;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Centralized message handler for RapunzelCore.
 *
 *
 * This class provides a convenient wrapper around RapunzelLib's message system,
 * offering simplified methods for retrieving formatted messages with placeholders.
 * All user-facing messages should go through this handler to ensure consistency
 * and support for localization.
 *
 *
 *
 * Usage examples:
 *
 * <pre>
 * MessageHandler messages = new MessageHandler();
 *
 * // Simple message
 * player.sendMessage(messages.getMessage("welcome"));
 *
 * // Message with string placeholders
 * player.sendMessage(messages.getMessage("player-joined", playerName));
 *
 * // Message with multiple placeholders
 * player.sendMessage(messages.getMessage("teleport-success", world, x, y, z));
 *
 * // Message with component placeholders
 * player.sendMessage(messages.getMessage("rank-up", rankComponent));
 *
 * // Message with custom placeholders
 * player.sendMessage(messages.getMessage("custom", ph ->
 *     ph.string("player", name).string("action", action)));
 * </pre>
 *
 * @see de.t14d3.rapunzellib.message.MessageFormatService
 * @see de.t14d3.rapunzellib.message.Placeholders
 */
public class MessageHandler {

/**
 * Creates a new MessageHandler instance and reloads messages from configuration.
 */
 public MessageHandler() {
 reloadMessages();
 }

/**
 * Reloads all messages from the configuration files.
 * This should be called after configuration changes to refresh cached messages.
 */
 public void reloadMessages() {
 messages().reload();
 }

/**
 * Retrieves a message by its key.
 *
 * @param key the message key defined in the configuration
 * @return the formatted message component, or an empty component if key not found
 * @throws NullPointerException if key is null
 */
 @NotNull
 public Component getMessage(@NotNull String key) {
 Objects.requireNonNull(key, "key cannot be null");
 return messages().component(key);
 }

/**
 * Retrieves a message with custom placeholders.
 *
 * @param key the message key
 * @param placeholders the placeholders to replace in the message
 * @return the formatted message component
 * @throws NullPointerException if key or placeholders is null
 */
 @NotNull
 public Component getMessage(@NotNull String key, @NotNull Placeholders placeholders) {
 Objects.requireNonNull(key, "key cannot be null");
 Objects.requireNonNull(placeholders, "placeholders cannot be null");
 return messages().component(key, placeholders);
 }

/**
 * Retrieves a message with placeholders built using a consumer.
 *
 * @param key the message key
 * @param builder consumer that configures the placeholders builder
 * @return the formatted message component
 * @throws NullPointerException if key or builder is null
 */
 @NotNull
 public Component getMessage(@NotNull String key, @NotNull Consumer<Placeholders.Builder> builder) {
 Objects.requireNonNull(key, "key cannot be null");
 Objects.requireNonNull(builder, "builder cannot be null");
 Placeholders.Builder ph = Placeholders.builder();
 builder.accept(ph);
 return getMessage(key, ph.build());
 }

/**
 * Retrieves a message with a single string argument.
 * The argument will be available as {arg1} in the message template.
 *
 * @param key the message key
 * @param arg1 the first argument value
 * @return the formatted message component
 * @throws NullPointerException if key is null
 */
 @NotNull
 public Component getMessage(@NotNull String key, @Nullable String arg1) {
 Objects.requireNonNull(key, "key cannot be null");
 return getMessage(key, new String[]{arg1});
 }

/**
 * Retrieves a message with multiple string arguments.
 * Arguments will be available as {arg1}, {arg2}, etc. in the message template.
 *
 * @param key the message key
 * @param args the argument values
 * @return the formatted message component
 * @throws NullPointerException if key is null
 */
 @NotNull
 public Component getMessage(@NotNull String key, @Nullable String... args) {
 Objects.requireNonNull(key, "key cannot be null");
 return getMessage(key, ph -> {
 if (args != null) {
 for (int i = 0; i < args.length; i++) {
 ph.string("arg" + (i + 1), args[i]);
 }
 }
 });
 }

/**
 * Retrieves a message with component arguments.
 * Components will be available as {arg1}, {arg2}, etc. in the message template.
 *
 * @param key the message key
 * @param args the component arguments
 * @return the formatted message component
 * @throws NullPointerException if key is null
 */
 @NotNull
 public Component getMessage(@NotNull String key, @Nullable Component... args) {
 Objects.requireNonNull(key, "key cannot be null");
 return getMessage(key, ph -> {
 if (args != null) {
 for (int i = 0; i < args.length; i++) {
 ph.component("arg" + (i + 1), args[i]);
 }
 }
 });
 }

/**
 * Retrieves the raw message string without formatting.
 * Useful for getting message keys for comparison or logging.
 *
 * @param key the message key
 * @return the raw message string, or empty string if key not found
 * @throws NullPointerException if key is null
 */
 @NotNull
 public String getRaw(@NotNull String key) {
 Objects.requireNonNull(key, "key cannot be null");
 return messages().raw(key);
 }

/**
 * Checks if a message key exists in the configuration.
 *
 * @param key the message key to check
 * @return true if the key exists, false otherwise
 * @throws NullPointerException if key is null
 */
 public boolean hasMessage(@NotNull String key) {
 Objects.requireNonNull(key, "key cannot be null");
 return !getRaw(key).isEmpty();
 }

/**
 * Gets the underlying message format service.
 *
 * @return the message format service instance
 */
 @NotNull
 private static MessageFormatService messages() {
 return Rapunzel.context().messages();
 }
}
