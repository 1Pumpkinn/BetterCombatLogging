package net.saturn.listeners.regions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import net.saturn.BetterCombatLogging;
import org.bukkit.Location;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import java.util.List;

public class RegionVehicleListener implements Listener {

    private final BetterCombatLogging plugin;

    public RegionVehicleListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Entity vehicle = event.getVehicle();

        // Only handle minecarts and boats
        if (!(vehicle instanceof Minecart) && !(vehicle instanceof Boat)) {
            return;
        }

        Location to = event.getTo();
        Location from = event.getFrom();

        // Only check if vehicle moved to a different block
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // Get blocked regions
        List<String> blockedRegions = plugin.getRegionManager().getBlockedRegions();
        if (blockedRegions.isEmpty()) {
            return;
        }

        // Check if vehicle is entering a blocked region
        if (isInBlockedRegion(to, blockedRegions) && !isInBlockedRegion(from, blockedRegions)) {
            // Check if any passenger is in combat
            boolean hasPlayerInCombat = false;
            Player combatPlayer = null;

            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof Player) {
                    Player player = (Player) passenger;
                    if (plugin.getCombatManager().isInCombat(player)) {
                        hasPlayerInCombat = true;
                        combatPlayer = player;
                        break;
                    }
                }
            }

            if (hasPlayerInCombat) {
                // Eject all passengers
                for (Entity passenger : vehicle.getPassengers()) {
                    vehicle.removePassenger(passenger);

                    if (passenger instanceof Player) {
                        Player player = (Player) passenger;

                        // Calculate knockback direction (away from the region)
                        Vector direction = from.toVector().subtract(to.toVector()).normalize();
                        direction.setY(0.5); // Add upward velocity
                        direction.multiply(1.5); // Increase knockback strength

                        player.setVelocity(direction);

                        if (plugin.getCombatManager().isInCombat(player)) {
                            int remaining = plugin.getCombatManager().getRemainingTime(player);
                            String message = plugin.getConfig().getString("messages.vehicle-blocked",
                                            "&cYou cannot enter this region in a vehicle while in combat! &7({time}s remaining)")
                                    .replace("{time}", String.valueOf(remaining));
                            player.sendMessage(colorize(message));
                        }
                    }
                }

                // Destroy the vehicle
                vehicle.remove();
            } else {
                // No players in combat, just destroy the vehicle silently
                vehicle.remove();
            }
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