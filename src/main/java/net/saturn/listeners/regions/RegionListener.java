package net.saturn.listeners.regions;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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

        // Get blocked regions from RegionManager
        List<String> blockedRegions = plugin.getRegionManager().getBlockedRegions();
        if (blockedRegions.isEmpty()) {
            return;
        }

        boolean isInBlockedRegionNow = isInBlockedRegion(to, blockedRegions);
        boolean wasInBlockedRegion = isInBlockedRegion(from, blockedRegions);

        // If player is trying to enter a blocked region, cancel movement
        if (isInBlockedRegionNow && !wasInBlockedRegion) {
            event.setCancelled(true);

            int remaining = combatManager.getRemainingTime(player);
            String message = plugin.getConfig().getString("messages.region-blocked",
                            "&cYou cannot enter this region while in combat! &7({time}s remaining)")
                    .replace("{time}", String.valueOf(remaining));
            player.sendMessage(colorize(message));
        }
        // If player is somehow already in a blocked region, eject them
        else if (isInBlockedRegionNow && wasInBlockedRegion) {
            ejectPlayerFromRegion(player, from, to, blockedRegions);
        }
    }

    private void ejectPlayerFromRegion(Player player, Location from, Location to, List<String> blockedRegions) {
        // Calculate direction away from the region center
        Location safeLocation = findSafeLocationOutsideRegion(player, from, to, blockedRegions);

        if (safeLocation != null) {
            // Teleport player to safe location
            player.teleport(safeLocation);

            // Add knockback effect away from region
            Vector direction = safeLocation.toVector().subtract(to.toVector()).normalize();
            direction.setY(0.3); // Add upward velocity
            direction.multiply(1.2); // Increase knockback strength

            // Apply velocity after a short delay to ensure teleport completes
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setVelocity(direction);
                    }
                }
            }.runTaskLater(plugin, 1L);

            int remaining = combatManager.getRemainingTime(player);
            String message = plugin.getConfig().getString("messages.region-ejected",
                            "&cYou were ejected from a blocked region! &7({time}s remaining)")
                    .replace("{time}", String.valueOf(remaining));
            player.sendMessage(colorize(message));
        }
    }

    private Location findSafeLocationOutsideRegion(Player player, Location from, Location current, List<String> blockedRegions) {
        // Try the previous location first
        if (!isInBlockedRegion(from, blockedRegions)) {
            return from;
        }

        // Search in a spiral pattern for a safe location
        Location searchStart = current.clone();
        int maxRadius = 10;

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    // Only check the outer edge of the current radius
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }

                    Location testLoc = searchStart.clone().add(x, 0, z);

                    // Find ground level
                    for (int y = -5; y <= 5; y++) {
                        Location groundTest = testLoc.clone().add(0, y, 0);

                        if (!isInBlockedRegion(groundTest, blockedRegions) && isSafeLocation(groundTest)) {
                            return groundTest;
                        }
                    }
                }
            }
        }

        // If no safe location found, return spawn or world spawn
        return player.getWorld().getSpawnLocation();
    }

    private boolean isSafeLocation(Location location) {
        // Check if the location has solid ground and air above
        return location.getBlock().getType().isSolid() &&
                location.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                location.clone().add(0, 2, 0).getBlock().getType().isAir();
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