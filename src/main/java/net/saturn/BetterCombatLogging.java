package net.saturn;

import net.saturn.commands.BlockedRegionCommand;
import net.saturn.commands.CleanupCommand;
import net.saturn.commands.combat.CombatCommand;
import net.saturn.commands.combat.CombatDurationCommand;
import net.saturn.commands.combat.CombatTestCommand;
import net.saturn.commands.ProtectionLimitCommand;
import net.saturn.listeners.CombatListener;
import net.saturn.listeners.ProtectionListener;
import net.saturn.listeners.RegionListener;
import net.saturn.managers.CombatManager;
import net.saturn.managers.ProtectionManager;
import net.saturn.managers.RegionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterCombatLogging extends JavaPlugin {

    private CombatManager combatManager;
    private ProtectionManager protectionManager;
    private RegionManager regionManager;
    private boolean worldGuardEnabled = false;

    @Override
    public void onEnable() {
        // Check for WorldGuard (optional for region-based features)
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("WorldGuard detected! Region-based combat restrictions enabled.");
        } else {
            getLogger().warning("WorldGuard not found! Region-based combat restrictions disabled.");
        }

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        combatManager = new CombatManager(this);
        protectionManager = new ProtectionManager(this);
        protectionManager.load();

        regionManager = new RegionManager(this);
        regionManager.load();

        // Register listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this, protectionManager), this);

        // Only register RegionListener if WorldGuard is present
        if (worldGuardEnabled) {
            getServer().getPluginManager().registerEvents(new RegionListener(this, combatManager), this);
        }

        // Register commands
        getCommand("combat").setExecutor(new CombatCommand(combatManager));
        getCommand("protectionlimit").setExecutor(new ProtectionLimitCommand(this, protectionManager));
        getCommand("blockedregion").setExecutor(new BlockedRegionCommand(this, regionManager));
        getCommand("activatecombat").setExecutor(new CombatTestCommand(combatManager));
        getCommand("setcombatduration").setExecutor(new CombatDurationCommand(this));
        getCommand("cleanup").setExecutor(new CleanupCommand(this));

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

        // Save blocked regions
        if (regionManager != null) {
            regionManager.save();
        }

        getLogger().info("BetterCombatLogging has been disabled!");
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}