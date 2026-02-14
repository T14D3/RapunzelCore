package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.Players;
import de.t14d3.spool.annotations.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity representing a player in the database.
 *
 *
 * This entity stores player-specific data including:
 *
 * - Player identity (UUID, name, display name)
 * - Teleport toggle preference
 * - Chat channel memberships
 * - Social spy settings
 * - Associated homes
 *
 *
 * The entity provides convenience methods for sending messages and checking permissions
 * through the RapunzelLib player service.
 *
 *
 * @see Home
 * @see Channel
 */
@Entity
@Table(name = "players")
public class PlayerEntity {

 private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

 @Id
 @Column(name = "uuid", nullable = false, type = "VARCHAR(36)")
 private String uuid;

 @Column(name = "display_name", nullable = true)
 private String displayName = "";

 @Column(name = "name", nullable = true)
 private String name = "";

 @OneToMany(targetEntity = Home.class, mappedBy = "playerEntity", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
 private List<Home> homes = new ArrayList<>();

 @Column(name = "tp_toggle", nullable = false, type = "BOOLEAN")
 private boolean tpToggle = false;

 @ManyToMany(fetch = FetchType.EAGER)
 @JoinTable(
 name = "player_channels",
 joinColumn = "player_uuid",
 inverseJoinColumn = "channel_id"
 )
 private Set<Channel> joinedChannels = new LinkedHashSet<>();

 @Column(name = "chat_main_channel")
 private String mainChannel = "";

 @Column(name = "chat_social_spy")
 private boolean socialSpyEnabled = false;

 @Column(name = "last_messaged_uuid")
 private String lastMessagedUuid;

/**
 * Default constructor required by JPA.
 */
 public PlayerEntity() {}

/**
 * Gets the player's UUID.
 *
 * @return the UUID, or null if not set
 */
 @Nullable
 public UUID getUuid() {
 if (uuid == null || uuid.isBlank()) return null;
 return UUID.fromString(uuid);
 }

/**
 * Sets the player's UUID.
 *
 * @param uuid the UUID to set
 */
 public void setUuid(@Nullable UUID uuid) {
 this.uuid = uuid == null ? null : uuid.toString();
 }

/**
 * Gets the player's name.
 *
 * @return the player name
 */
 @NotNull
 public String getName() {
 return name != null ? name : "";
 }

/**
 * Sets the player's name.
 *
 * @param name the name to set
 */
 public void setName(@Nullable String name) {
 this.name = name;
 }

/**
 * Gets the player's display name as a Component.
 *
 * @return the display name component
 */
 @NotNull
 public Component getDisplayName() {
 return PLAIN_SERIALIZER.deserialize(this.displayName != null ? this.displayName : "");
 }

/**
 * Sets the player's display name from a Component.
 *
 * @param displayName the display name component
 */
 public void setDisplayName(@Nullable Component displayName) {
 this.displayName = displayName != null ? PLAIN_SERIALIZER.serialize(displayName) : "";
 }

/**
 * Gets the player's homes.
 *
 * @return list of homes
 */
 @NotNull
 public List<Home> getHomes() {
 return homes != null ? homes : new ArrayList<>();
 }

/**
 * Adds a home for this player.
 *
 * @param home the home to add
 */
 public void addHome(@NotNull Home home) {
 if (this.homes == null) {
 this.homes = new ArrayList<>();
 }
 this.homes.add(home);
 home.setPlayer(this);
 }

/**
 * Checks if teleport requests are enabled for this player.
 *
 * @return true if teleport requests are enabled
 */
 public boolean isTpToggle() {
 return tpToggle;
 }

/**
 * Sets the teleport toggle state.
 *
 * @param tpToggle true to enable teleport requests
 */
 public void setTpToggle(boolean tpToggle) {
 this.tpToggle = tpToggle;
 }

/**
 * Gets the channels this player has joined.
 *
 * @return set of joined channels
 */
 @NotNull
 public Set<Channel> getJoinedChannels() {
 return joinedChannels != null ? joinedChannels : new LinkedHashSet<>();
 }

/**
 * Sets the joined channels for this player.
 *
 * @param joinedChannels the channels to set
 */
 public void setJoinedChannels(@Nullable Set<Channel> joinedChannels) {
 this.joinedChannels = (joinedChannels != null) ? joinedChannels : new LinkedHashSet<>();
 }

/**
 * Gets the player's main chat channel.
 *
 * @return the main channel name
 */
 @NotNull
 public String getMainChannel() {
 return mainChannel != null ? mainChannel : "";
 }

/**
 * Sets the player's main chat channel.
 *
 * @param mainChannel the channel name
 */
 public void setMainChannel(@Nullable String mainChannel) {
 this.mainChannel = mainChannel;
 }

/**
 * Checks if social spy is enabled for this player.
 *
 * @return true if social spy is enabled
 */
 public boolean isSocialSpyEnabled() {
 return socialSpyEnabled;
 }

/**
 * Sets the social spy state.
 *
 * @param socialSpyEnabled true to enable social spy
 */
 public void setSocialSpyEnabled(boolean socialSpyEnabled) {
   this.socialSpyEnabled = socialSpyEnabled;
 }

 /**
  * Gets the UUID of the last player this player messaged or was messaged by.
  *
  * @return the last messaged player UUID as a string, or null if none
  */
 @Nullable
 public String getLastMessagedUuid() {
   return lastMessagedUuid;
 }

 /**
  * Sets the UUID of the last player this player messaged or was messaged by.
  *
  * @param lastMessagedUuid the UUID string to set
  */
 public void setLastMessagedUuid(@Nullable String lastMessagedUuid) {
   this.lastMessagedUuid = lastMessagedUuid;
 }

/**
 * Sends a message to this player if they are online.
 *
 * @param message the message to send
 */
 public void sendMessage(@NotNull Component message) {
 if (!Rapunzel.isBootstrapped()) return;
 UUID id = getUuid();
 if (id == null) return;
 Players players = Rapunzel.context().services().get(Players.class);
 players.get(id).ifPresent(p -> p.sendMessage(message));
 }

/**
 * Checks if this player has a specific permission.
 *
 * @param permission the permission to check
 * @return true if the player has the permission
 */
 public boolean hasPermission(@NotNull String permission) {
 if (!Rapunzel.isBootstrapped()) return false;
 UUID id = getUuid();
 if (id == null) return false;
 Players players = Rapunzel.context().services().get(Players.class);
 return players.get(id).map(p -> p.hasPermission(permission)).orElse(false);
 }

 @Override
 public boolean equals(Object o) {
 if (this == o) return true;
 if (!(o instanceof PlayerEntity playerEntity)) return false;
 return uuid != null && uuid.equals(playerEntity.uuid);
 }

 @Override
 public int hashCode() {
 return uuid == null ? 0 : uuid.hashCode();
 }
}
