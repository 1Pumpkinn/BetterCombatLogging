package net.saturn.tasks.cleanup;

import net.saturn.BetterCombatLogging;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemClearTask extends BukkitRunnable {

    private final BetterCombatLogging plugin;
    private int countdown;
    private final int intervalSeconds;
    private final int warningSeconds;

    public ItemClearTask(BetterCombatLogging plugin) {
        this.plugin = plugin;
        int intervalMinutes = plugin.getConfig().getInt("item-clear.interval-minutes", 20);
        this.intervalSeconds = intervalMinutes * 60;
        this.warningSeconds = plugin.getConfig().getInt("item-clear.warning-seconds", 30);
        this.countdown = intervalSeconds;
    }

    @Override
    public void run() {
        countdown--;

        // Warning announcement
        if (plugin.getConfig().getBoolean("item-clear.announce-warning", true) && countdown == warningSeconds) {
            String warningMessage = plugin.getConfig().getString("messages.item-clear-warning",
                            "&eItems will be cleared in {time} seconds!")
                    .replace("{time}", String.valueOf(warningSeconds));
            Bukkit.broadcastMessage(colorize(warningMessage));
        }

        // Clear items
        if (countdown <= 0) {
            clearAllItems();
            countdown = intervalSeconds; // Reset countdown
        }
    }

    private void clearAllItems() {
        int count = 0;

        // Clear items in all worlds
        for (World world : Bukkit.getWorlds()) {
            // Skip if world is disabled for item clearing
            if (!plugin.getConfig().getBoolean("item-clear.worlds." + world.getName() + ".enabled", true)) {
                continue;
            }

            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
                count++;
            }
        }

        // Broadcast clear message
        if (plugin.getConfig().getBoolean("item-clear.announce-clear", true)) {
            String clearMessage = plugin.getConfig().getString("messages.item-clear-complete",
                            "&aCleared {count} items from the ground!")
                    .replace("{count}", String.valueOf(count));
            Bukkit.broadcastMessage(colorize(clearMessage));
        }

        plugin.getLogger().info("Item clear completed. Removed " + count + " items.");
    }

    public void resetCountdown() {
        this.countdown = intervalSeconds;
    }

    public int getCountdown() {
        return countdown;
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}