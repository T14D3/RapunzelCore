package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.spool.annotations.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return uuid != null && uuid.equals(player.uuid);
    }

    @Override
    public int hashCode() {
        return uuid == null ? 0 : uuid.hashCode();
    }

    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    @Id
    @Column(name = "uuid", nullable = false, type = "VARCHAR(36)")
    private String uuid;

    @Column(name = "display_name", nullable = true)
    private String displayName = "";

    @Column(name = "name", nullable = true)
    private String name = "";

    @OneToMany(targetEntity = Home.class, mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
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
    private String mainChannel = ""; // Channel name

    @Column(name = "chat_social_spy")
    private boolean socialSpyEnabled = false;

    public List<Home> getHomes() {
        return homes;
    }

    public Player() {}

    public void setDisplayName(Component displayName) {
        this.displayName = PLAIN_SERIALIZER.serialize(displayName);
    }

    public Component getDisplayName() {
        return PLAIN_SERIALIZER.deserialize(this.displayName);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid == null ? null : uuid.toString();
    }

    public void addHome(Home home) {
        this.homes.add(home);
        home.setPlayer(this);
    }

    public boolean isTpToggle() {
        return tpToggle;
    }

    public void setTpToggle(boolean tpToggle) {
        this.tpToggle = tpToggle;
    }


    public String getMainChannel() {
        return mainChannel;
    }

    public void setMainChannel(String mainChannel) {
        this.mainChannel = mainChannel;
    }

    public boolean isSocialSpyEnabled() {
        return socialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean socialSpyEnabled) {
        this.socialSpyEnabled = socialSpyEnabled;
    }

    public void sendMessage(Component message) {
        RapunzelCore.getInstance().getPlatformManager().sendMessage(this, message);
    }

    public UUID getUuid() {
        if (uuid == null || uuid.isBlank()) return null;
        return UUID.fromString(uuid);
    }

    public Set<Channel> getJoinedChannels() {
        return joinedChannels;
    }

    public void setJoinedChannels(Set<Channel> joinedChannels) {
        this.joinedChannels = (joinedChannels != null) ? joinedChannels : new LinkedHashSet<>();
    }

    public boolean hasPermission(String permission) {
        return RapunzelCore.getInstance().getPlatformManager().hasPermission(this, permission);
    }
}
