package de.t14d3.rapunzelcore.database.entities;

import de.t14d3.spool.annotations.*;
import de.t14d3.rapunzellib.config.YamlConfig;

/**
 * Unified database entity for all chat channels.
 * Supports different channel types through the type field.
 */
@Entity
@Table(name = "channels")
public class Channel {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Channel channel)) return false;
        // Channel id is its name
        return name != null && name.equalsIgnoreCase(channel.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.toLowerCase().hashCode();
    }

    @Id(autoIncrement = false)
    @Column(name = "id")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "format")
    private String format;

    @Column(name = "shortcut")
    private String shortcut;

    @Column(name = "permission")
    private String permission;

    @Column(name = "cross_server")
    private boolean crossServer;

    @Column(name = "range")
    private double range = 200.0;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @ManyToMany(mappedBy = "joinedChannels", fetch = FetchType.EAGER)
    private java.util.Set<PlayerEntity> members = new java.util.LinkedHashSet<>();

    public Channel() {}

    public Channel(String name, ChannelType type, String format, String shortcut, String permission, boolean crossServer) {
        this.name = name;
        this.type = type.name();
        this.format = format;
        this.shortcut = shortcut;
        this.permission = permission;
        this.crossServer = crossServer;
    }

    public Channel(String name, ChannelType type, String format, String shortcut, String permission, boolean crossServer, double range) {
        this(name, type, format, shortcut, permission, crossServer);
        this.range = range;
    }

    public boolean hasPermission(PlayerEntity playerEntity) {
        if (permission == null || permission.trim().isEmpty()) {
            return true;
        }
        return playerEntity.hasPermission(permission);
    }

    // Static factory method for creating from configuration
    public static Channel fromConfig(String channelName, YamlConfig config) {
        String path = "channels." + channelName;
        String typeStr = config.getString(path + ".type", "global").toUpperCase();
        ChannelType type = ChannelType.valueOf(typeStr);
        String permission = config.getString(path + ".permission", "");
        String format = config.getString(path + ".format", "<prefix>[<name>]<reset> <message>");
        String shortcut = config.getString(path + ".shortcut", "");
        boolean crossServer = config.getBoolean(path + ".crossServer", false);
        double range = config.getDouble(path + ".range", 200.0);
        boolean isDefault = config.getBoolean(path + ".default", false);

        Channel channel = new Channel(channelName, type, format, shortcut, permission, crossServer, range);
        channel.setDefault(isDefault);
        
        return channel;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public String getId() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChannelType getType() {
        if (type == null || type.isBlank()) {
            return ChannelType.GLOBAL;
        }
        return ChannelType.valueOf(type.toUpperCase());
    }

    public void setType(ChannelType type) {
        this.type = type.name();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public boolean isCrossServer() {
        return crossServer;
    }

    public void setCrossServer(boolean crossServer) {
        this.crossServer = crossServer;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * Inverse side of player.joinedChannels.
     *
     * For channel types that are implicit (GLOBAL/LOCAL/PERMISSION), this collection is not used.
     */
    public java.util.Set<PlayerEntity> getMembers() {
        return members;
    }

    public void setMembers(java.util.Set<PlayerEntity> members) {
        this.members = (members != null) ? members : new java.util.LinkedHashSet<>();
    }

    /**
     * Channel type enum for different channel behaviors
     */
    public enum ChannelType {
        GLOBAL,      // All online players
        LOCAL,       // Players within a certain range
        PERMISSION,  // Players with specific permission
        PLAYER       // Only allowed players (managed channel)
    }
}
