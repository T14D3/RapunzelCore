package de.t14d3.rapunzelcore.modules.script;

import de.t14d3.rapunzelcore.Module;
import net.milkbowl.vault.economy.Economy;

import de.t14d3.rapunzelcore.RapunzelPaperCore;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyManager {
    private final Module module;
    private Economy economy;
    private boolean enabled = false;

    public VaultEconomyManager(Module module) {
        this.module = module;
    }

    public void enable() {
        if (enabled) return;

        // Check if Vault is available
        RegisteredServiceProvider<Economy> economyProvider = 
            ((RapunzelPaperCore) module.getCore()).getServer().getServicesManager().getRegistration(Economy.class);
        
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            enabled = true;
            ((RapunzelPaperCore) module.getCore()).getLogger().info("Vault economy integration enabled");
        } else {
            ((RapunzelPaperCore) module.getCore()).getLogger().warning("Vault not found - economy features disabled");
        }
    }

    public void disable() {
        economy = null;
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasAccount(String playerName) {
        if (!enabled || economy == null) return false;
        return economy.hasAccount(playerName);
    }

    public double getBalance(String playerName) {
        if (!enabled || economy == null) return 0.0;
        return economy.getBalance(playerName);
    }

    public boolean withdraw(String playerName, double amount) {
        if (!enabled || economy == null) return false;
        return economy.withdrawPlayer(playerName, amount).transactionSuccess();
    }

    public boolean deposit(String playerName, double amount) {
        if (!enabled || economy == null) return false;
        return economy.depositPlayer(playerName, amount).transactionSuccess();
    }
}
