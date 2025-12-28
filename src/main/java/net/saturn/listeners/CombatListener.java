package net.saturn.listeners;

import net.saturn.BetterCombatLogging;
import net.saturn.managers.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {

    private final BetterCombatLogging plugin;
    private final CombatManager combatManager;

    public CombatListener(BetterCombatLogging plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = null;

        // Direct damage from player
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }
        // Damage from projectile
        else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        // Check if PvP is disabled in config
        if (!plugin.getConfig().getBoolean("enable-pvp-combat", true)) {
            return;
        }

        // Tag both players
        combatManager.tagPlayer(victim);
        combatManager.tagPlayer(attacker);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (combatManager.isInCombat(player)) {
            // Kill the player
            player.setHealth(0.0);

            // Broadcast message
            if (plugin.getConfig().getBoolean("broadcast-combat-log", true)) {
                String message = plugin.getConfig().getString("messages.combat-logout", "&e{player} &clogged out during combat!")
                        .replace("{player}", player.getName());
                Bukkit.broadcastMessage(colorize(message));
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Remove combat tag on death
        if (combatManager.isInCombat(player)) {
            combatManager.removeTag(player);
        }
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}