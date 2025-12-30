package net.saturn.managers;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final net.saturn.BetterCombatLogging plugin;
    private final Map<UUID, Long> combatTags;
    private final Map<UUID, BossBar> bossBars;
    private final Map<UUID, BukkitTask> bossBarTasks;
    private final int combatDuration;

    public CombatManager(net.saturn.BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.combatTags = new HashMap<>();
        this.bossBars = new HashMap<>();
        this.bossBarTasks = new HashMap<>();
        this.combatDuration = plugin.getConfig().getInt("combat-duration", 15);
    }

    public void tagPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime + (combatDuration * 1000L);

        boolean wasInCombat = isInCombat(player);

        // Always update the expire time to reset the timer
        combatTags.put(uuid, expireTime);

        if (!wasInCombat) {
            String enterMessage = plugin.getConfig().getString("messages.combat-enter", "&cYou are now in combat!");
            player.sendMessage(colorize(enterMessage));
            startBossBarTimer(player);
        }
        // If already in combat, the timer will be updated in the next tick of the BossBar task
    }

    public boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTags.containsKey(uuid)) {
            return false;
        }

        long expireTime = combatTags.get(uuid);
        if (System.currentTimeMillis() >= expireTime) {
            removeTag(player);
            return false;
        }

        return true;
    }

    public int getRemainingTime(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTags.containsKey(uuid)) {
            return 0;
        }

        long expireTime = combatTags.get(uuid);
        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return (int) Math.max(0, remaining);
    }

    public void removeTag(Player player) {
        UUID uuid = player.getUniqueId();
        if (combatTags.remove(uuid) != null) {
            String exitMessage = plugin.getConfig().getString("messages.combat-exit", "&aYou are no longer in combat!");
            player.sendMessage(colorize(exitMessage));
            stopBossBarTimer(player);
        }
    }

    private void startBossBarTimer(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel existing task and boss bar if any
        stopBossBarTimer(player);

        // Create boss bar
        String bossBarTitle = plugin.getConfig().getString("messages.boss-bar", "&c⚔ Combat: {time}s");
        BossBar bossBar = Bukkit.createBossBar(
                colorize(bossBarTitle.replace("{time}", String.valueOf(combatDuration))),
                BarColor.RED,
                BarStyle.SOLID
        );
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        bossBars.put(uuid, bossBar);

        // Start update task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    stopBossBarTimer(player);
                    return;
                }

                int remaining = getRemainingTime(player);
                if (remaining <= 0) {
                    removeTag(player);
                    cancel();
                    return;
                }

                // Update boss bar title
                String title = plugin.getConfig().getString("messages.boss-bar", "&c⚔ Combat: {time}s")
                        .replace("{time}", String.valueOf(remaining));
                bossBar.setTitle(colorize(title));

                // Update boss bar progress (percentage of time remaining)
                double progress = (double) remaining / combatDuration;
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                // Change color based on remaining time
                if (remaining <= 5) {
                    bossBar.setColor(BarColor.YELLOW);
                } else if (remaining <= 10) {
                    bossBar.setColor(BarColor.RED);
                } else {
                    bossBar.setColor(BarColor.RED);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        bossBarTasks.put(uuid, task);
    }

    private void stopBossBarTimer(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel task
        BukkitTask task = bossBarTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        // Remove boss bar
        BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            bossBar.setVisible(false);
        }
    }

    public void shutdown() {
        // Cancel all boss bar tasks
        for (BukkitTask task : bossBarTasks.values()) {
            task.cancel();
        }
        bossBarTasks.clear();

        // Remove all boss bars
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        bossBars.clear();

        combatTags.clear();
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}