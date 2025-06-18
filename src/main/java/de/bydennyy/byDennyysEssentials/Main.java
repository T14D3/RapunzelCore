package de.bydennyy.byDennyysEssentials;

import de.bydennyy.byDennyysEssentials.command.AliasCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class Main extends JavaPlugin {

    public static Manager manager;

    @Getter
    public static LuckPerms luckPerms;

    @Override
    public void onEnable() {
        manager = new Manager(this);
        manager.initialize();
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        }


        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(AliasCommand.createCommand()));

    }
}