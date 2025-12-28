package net.saturn.listeners;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.saturn.BetterCombatLogging;
import net.saturn.managers.CombatManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class RegionListener implements Listener {

    private final BetterCombatLogging plugin;
    private final CombatManager combatManager;

    public RegionListener(BetterCombatLogging plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player moved to a different block
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();

        // Only check if player is in combat
        if (!combatManager.isInCombat(player)) {
            return;
        }

        // Get blocked regions from config
        List<String> blockedRegions = plugin.getConfig().getStringList("blocked-regions");
        if (blockedRegions.isEmpty()) {
            return;
        }

        // Check if player is entering a blocked region
        if (isInBlockedRegion(to, blockedRegions) && !isInBlockedRegion(from, blockedRegions)) {
            event.setCancelled(true);

            int remaining = combatManager.getRemainingTime(player);
            String message = plugin.getConfig().getString("messages.region-blocked",
                            "&cYou cannot enter this region while in combat! &7({time}s remaining)")
                    .replace("{time}", String.valueOf(remaining));
            player.sendMessage(colorize(message));
        }
    }

    private boolean isInBlockedRegion(Location location, List<String> blockedRegions) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));

        for (ProtectedRegion region : set) {
            if (blockedRegions.contains(region.getId())) {
                return true;
            }
        }

        return false;
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}