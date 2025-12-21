package de.t14d3.rapunzelcore.modules.teleports;

import de.t14d3.rapunzelcore.Environment;
import de.t14d3.rapunzelcore.Module;
import de.t14d3.rapunzelcore.RapunzelCore;
import de.t14d3.rapunzelcore.RapunzelPaperCore;
import de.t14d3.rapunzelcore.database.CoreDatabase;
import de.t14d3.rapunzelcore.modules.commands.Command;
import org.simpleyaml.configuration.file.FileConfiguration;

import java.util.Map;

public class TeleportsModule implements Module {
    private boolean enabled = false;
    private RapunzelCore core;
    private CoreDatabase coreDatabase;
    private FileConfiguration config;

    private Command warpCommands;
    private Command tpaCommands;
    private Command homeCommands;

    @Override
    public Environment getEnvironment() {
        return Environment.BOTH;
    }

    @Override
    public void enable(RapunzelCore core, Environment environment) {
        if (enabled) return;
        this.core = core;
        this.coreDatabase = core.getCoreDatabase();
        this.config = loadConfig();
        enabled = true;

        if (environment == Environment.PAPER) {
            RapunzelPaperCore main = (RapunzelPaperCore) core;
            this.warpCommands = new WarpCommands(main);
            this.tpaCommands = new TpaCommands(main);
            this.homeCommands = new HomeCommands(main);
        }
    }

    @Override
    public void disable(RapunzelCore core, Environment environment) {
        if (!enabled) return;
        // Commands are automatically unregistered
        if (environment == Environment.PAPER) {
            warpCommands.unregister();
            tpaCommands.unregister();
            homeCommands.unregister();
        }
    }

    @Override
    public String getName() {
        return "teleports";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> getPermissions() {
        return Map.ofEntries(
                Map.entry("rapunzelcore.home", "op"),
                Map.entry("rapunzelcore.sethome", "op"),
                Map.entry("rapunzelcore.delhome", "op"),
                Map.entry("rapunzelcore.homes", "op"),
                Map.entry("rapunzelcore.homes.unlimited", "op"),
                Map.entry("rapunzelcore.homes.*", "op"),
                Map.entry("rapunzelcore.warp", "op"),
                Map.entry("rapunzelcore.setwarp", "op"),
                Map.entry("rapunzelcore.delwarp", "op"),
                Map.entry("rapunzelcore.warps", "op"),
                Map.entry("rapunzelcore.spawn", "op"),
                Map.entry("rapunzelcore.setspawn", "op"),
                Map.entry("rapunzelcore.tpa", "op"),
                Map.entry("rapunzelcore.tpahere", "op"),
                Map.entry("rapunzelcore.tpaccept", "op"),
                Map.entry("rapunzelcore.tpdeny", "op"),
                Map.entry("rapunzelcore.tptoggle", "op"),
                Map.entry("rapunzelcore.tphere", "op"),
                Map.entry("rapunzelcore.tpo", "op"),
                Map.entry("rapunzelcore.tpohere", "op")
        );
    }
    public TeleportsModule() {

    }
}
