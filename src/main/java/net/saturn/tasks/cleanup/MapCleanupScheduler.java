package net.saturn.tasks.cleanup;

import net.saturn.BetterCombatLogging;
import org.bukkit.scheduler.BukkitRunnable;

public class MapCleanupScheduler extends BukkitRunnable {

    private final BetterCombatLogging plugin;

    public MapCleanupScheduler(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("cleanup.enabled", true)) {
            return;
        }

        plugin.getLogger().info("Starting scheduled map cleanup...");

        // Run the cleanup task
        MapCleanupTask cleanupTask = new MapCleanupTask(plugin);
        cleanupTask.runTask(plugin);
    }
}