package de.t14d3.rapunzelcore.entities;

import de.t14d3.spool.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @OneToMany(targetEntity = Home.class, mappedBy = "player", cascade = CascadeType.ALL)
    private List<Home> homes = new ArrayList<>();

    @Column(name = "tp_toggle")
    private boolean tpToggle = false;

    public List<Home> getHomes() {
        return homes;
    }

    public Player() {}

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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
}
