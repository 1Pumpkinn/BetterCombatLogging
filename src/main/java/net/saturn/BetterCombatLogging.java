package net.saturn;

import net.saturn.commands.CombatCommand;
import net.saturn.commands.ProtectionLimitCommand;
import net.saturn.listeners.CombatListener;
import net.saturn.listeners.ProtectionListener;
import net.saturn.listeners.RegionListener;
import net.saturn.managers.CombatManager;
import net.saturn.managers.ProtectionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterCombatLogging extends JavaPlugin {

    private CombatManager combatManager;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        // Check for WorldGuard
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            getLogger().severe("WorldGuard not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        combatManager = new CombatManager(this);
        protectionManager = new ProtectionManager(this);
        protectionManager.load();

        // Register listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new RegionListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this, protectionManager), this);

        // Register commands
        getCommand("combat").setExecutor(new CombatCommand(combatManager));
        getCommand("protectionlimit").setExecutor(new ProtectionLimitCommand(this, protectionManager));

        getLogger().info("BetterCombatLogging has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel all combat timers
        if (combatManager != null) {
            combatManager.shutdown();
        }

        // Save protection limits
        if (protectionManager != null) {
            protectionManager.save();
        }

        getLogger().info("BetterCombatLogging has been disabled!");
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}