package net.saturn.betterCombatLogger.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager {

    private final net.saturn.betterCombatLogger.BetterCombatLogger plugin;
    private final Map<UUID, Long> combatTags;
    private final Map<UUID, BukkitTask> actionBarTasks;
    private final int combatDuration;

    public CombatManager(net.saturn.betterCombatLogger.BetterCombatLogger plugin) {
        this.plugin = plugin;
        this.combatTags = new HashMap<>();
        this.actionBarTasks = new HashMap<>();
        this.combatDuration = plugin.getConfig().getInt("combat-duration", 15);
    }

    public void tagPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        long expireTime = System.currentTimeMillis() + (combatDuration * 1000L);

        boolean wasInCombat = isInCombat(player);
        combatTags.put(uuid, expireTime);

        if (!wasInCombat) {
            String enterMessage = plugin.getConfig().getString("messages.combat-enter", "&cYou are now in combat!");
            player.sendMessage(colorize(enterMessage));
            startActionBarTimer(player);
        }
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
            stopActionBarTimer(player);
        }
    }

    private void startActionBarTimer(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel existing task if any
        stopActionBarTimer(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                int remaining = getRemainingTime(player);
                if (remaining <= 0) {
                    removeTag(player);
                    cancel();
                    return;
                }

                String actionBar = plugin.getConfig().getString("messages.action-bar", "&c⚔ Combat: {time}s")
                        .replace("{time}", String.valueOf(remaining));
                player.sendActionBar(colorize(actionBar));
            }
        }.runTaskTimer(plugin, 0L, 20L);

        actionBarTasks.put(uuid, task);
    }

    private void stopActionBarTimer(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = actionBarTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        // Cancel all action bar tasks
        for (BukkitTask task : actionBarTasks.values()) {
            task.cancel();
        }
        actionBarTasks.clear();
        combatTags.clear();
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}