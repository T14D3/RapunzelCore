package de.t14d3.rapunzelcore.entities;

import de.t14d3.spool.annotations.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@Entity
public class Home {
    @Id(autoIncrement = true)
    @Column(name = "id")
    public long id;

    @Column(name = "name")
    public String name;

    @Column(name = "world")
    public String world;

    @Column(name = "x")
    public double x;

    @Column(name = "y")
    public double y;

    @Column(name = "z")
    public double z;

    @Column(name = "pitch")
    public float pitch;

    @Column(name = "yaw")
    public float yaw;

    @ManyToOne(cascade = CascadeType.ALL)
    public Player player;

    public Home() {}


    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }
}
