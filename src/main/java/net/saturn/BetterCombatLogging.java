package net.saturn;

import net.saturn.commands.BlockedRegionCommand;
import net.saturn.commands.cleanup.MapCleanCommand;
import net.saturn.commands.cleanup.ItemClearCommand;
import net.saturn.commands.combat.CombatCommand;
import net.saturn.commands.combat.CombatDurationCommand;
import net.saturn.commands.combat.CombatTestCommand;
import net.saturn.commands.limitations.ProtectionLimitCommand;
import net.saturn.limiter.listener.ItemLimitListener;
import net.saturn.limiter.manager.ItemLimitManager;
import net.saturn.listeners.CombatListener;
import net.saturn.listeners.ProtectionListener;
import net.saturn.listeners.regions.InteractionListener;
import net.saturn.managers.CombatManager;
import net.saturn.managers.ProtectionManager;
import net.saturn.managers.regions.RegionBorderVisualizer;
import net.saturn.managers.regions.RegionManager;
import net.saturn.tasks.cleanup.ItemClearTask;
import net.saturn.tasks.cleanup.MapCleanupScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterCombatLogging extends JavaPlugin {

    private CombatManager combatManager;
    private ProtectionManager protectionManager;
    private ItemLimitManager itemLimitManager;
    private RegionManager regionManager;
    private RegionBorderVisualizer regionVisualizer;
    private ItemClearTask itemClearTask;
    private MapCleanupScheduler mapCleanupScheduler;
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

        itemLimitManager = new ItemLimitManager(this);
        itemLimitManager.load();

        regionManager = new RegionManager(this);
        regionManager.load();

        // Initialize region visualizer if WorldGuard is enabled
        if (worldGuardEnabled && getConfig().getBoolean("region-visualizer.enabled", true)) {
            regionVisualizer = new RegionBorderVisualizer(this);
            regionVisualizer.start();
            getLogger().info("Region border visualizer started with optimized rendering!");
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this, combatManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this, protectionManager), this);
        getServer().getPluginManager().registerEvents(new ItemLimitListener(this, itemLimitManager), this);
        getServer().getPluginManager().registerEvents(new net.saturn.limiter.listener.VillagerTradeListener(this, itemLimitManager), this);

        // Only register RegionListener if WorldGuard is present
        if (worldGuardEnabled) {
            getServer().getPluginManager().registerEvents(new net.saturn.listeners.regions.RegionListener(this, combatManager), this);
            getServer().getPluginManager().registerEvents(new net.saturn.listeners.regions.RegionVisualizerListener(this), this);
            getServer().getPluginManager().registerEvents(new net.saturn.listeners.regions.RegionVehicleListener(this), this);
            getServer().getPluginManager().registerEvents(new net.saturn.listeners.regions.RegionBlockBreakListener(this), this);
            getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        }

        // Start item clear task if enabled
        if (getConfig().getBoolean("item-clear.enabled", true)) {
            itemClearTask = new ItemClearTask(this);
            itemClearTask.runTaskTimer(this, 0L, 20L); // Run every second
            getLogger().info("Item clear task started!");
        }

        // Start map cleanup scheduler if enabled
        if (getConfig().getBoolean("cleanup.enabled", true)) {
            int intervalMinutes = getConfig().getInt("cleanup.interval-minutes", 30);
            long intervalTicks = intervalMinutes * 60L * 20L; // Convert minutes to ticks

            mapCleanupScheduler = new MapCleanupScheduler(this);
            mapCleanupScheduler.runTaskTimer(this, intervalTicks, intervalTicks);
            getLogger().info("Map cleanup scheduler started! Running every " + intervalMinutes + " minutes.");
        }

        // Register commands
        getCommand("combat").setExecutor(new CombatCommand(combatManager));
        getCommand("protectionlimit").setExecutor(new ProtectionLimitCommand(this, protectionManager));
        getCommand("itemlimit").setExecutor(new net.saturn.limiter.command.ItemLimitCommand(this, itemLimitManager));
        getCommand("blockedregion").setExecutor(new BlockedRegionCommand(this, regionManager));
        getCommand("activatecombat").setExecutor(new CombatTestCommand(combatManager));
        getCommand("setcombatduration").setExecutor(new CombatDurationCommand(this));
        getCommand("mapclean").setExecutor(new MapCleanCommand(this));
        getCommand("itemclear").setExecutor(new ItemClearCommand(this));
        getCommand("togglevisualizer").setExecutor(new net.saturn.commands.ToggleVisualizerCommand(this));

        getLogger().info("BetterCombatLogging has been enabled with optimized performance!");
    }

    @Override
    public void onDisable() {
        // Stop region visualizer
        if (regionVisualizer != null) {
            regionVisualizer.stop();
        }

        // Cancel all combat timers
        if (combatManager != null) {
            combatManager.shutdown();
        }

        // Save protection limits
        if (protectionManager != null) {
            protectionManager.save();
        }

        // Save item limits
        if (itemLimitManager != null) {
            itemLimitManager.save();
        }

        // Save blocked regions
        if (regionManager != null) {
            regionManager.save();
        }

        // Cancel item clear task
        if (itemClearTask != null) {
            itemClearTask.cancel();
        }

        // Cancel map cleanup scheduler
        if (mapCleanupScheduler != null) {
            mapCleanupScheduler.cancel();
        }

        getLogger().info("BetterCombatLogging has been disabled!");
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public ItemLimitManager getItemLimitManager() {
        return itemLimitManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public RegionBorderVisualizer getRegionVisualizer() {
        return regionVisualizer;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }
}