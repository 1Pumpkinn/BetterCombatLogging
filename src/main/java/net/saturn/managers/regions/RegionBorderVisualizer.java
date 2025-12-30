package net.saturn.managers.regions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.saturn.BetterCombatLogging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RegionBorderVisualizer {

    private final BetterCombatLogging plugin;
    private final Map<UUID, Set<Location>> playerVisibleBlocks;
    private BukkitRunnable updateTask;

    public RegionBorderVisualizer(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.playerVisibleBlocks = new HashMap<>();
    }

    public void start() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePlayerView(player);
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 10L); // Update every 0.5 seconds
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        // Clear all fake blocks for all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearPlayerView(player);
        }
        playerVisibleBlocks.clear();
    }

    private void updatePlayerView(Player player) {
        UUID uuid = player.getUniqueId();

        // Clear old blocks
        Set<Location> oldBlocks = playerVisibleBlocks.getOrDefault(uuid, new HashSet<>());

        // Get new blocks to show
        Set<Location> newBlocks = new HashSet<>();

        // Only show if player is in combat
        if (plugin.getCombatManager().isInCombat(player)) {
            List<String> blockedRegions = plugin.getRegionManager().getBlockedRegions();

            if (!blockedRegions.isEmpty()) {
                RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                        .getRegionContainer()
                        .get(BukkitAdapter.adapt(player.getWorld()));

                if (regionManager != null) {
                    int distance = plugin.getConfig().getInt("region-visualizer.distance", 5);

                    for (String regionName : blockedRegions) {
                        ProtectedRegion region = regionManager.getRegion(regionName);
                        if (region != null && isPlayerNearRegion(player.getLocation(), region, distance)) {
                            newBlocks.addAll(getRegionBorderBlocks(region, player.getLocation()));
                        }
                    }
                }
            }
        }

        // Remove blocks that are no longer visible
        for (Location loc : oldBlocks) {
            if (!newBlocks.contains(loc)) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }

        // Add new blocks
        for (Location loc : newBlocks) {
            if (!oldBlocks.contains(loc)) {
                player.sendBlockChange(loc, Material.RED_STAINED_GLASS_PANE.createBlockData());
            }
        }

        playerVisibleBlocks.put(uuid, newBlocks);
    }

    public void clearPlayerView(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Location> blocks = playerVisibleBlocks.remove(uuid);

        if (blocks != null) {
            for (Location loc : blocks) {
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
    }

    private boolean isPlayerNearRegion(Location playerLoc, ProtectedRegion region, int distance) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        // Calculate the closest point in the region to the player
        double closestX = Math.max(min.getX(), Math.min(playerLoc.getX(), max.getX()));
        double closestY = Math.max(min.getY(), Math.min(playerLoc.getY(), max.getY()));
        double closestZ = Math.max(min.getZ(), Math.min(playerLoc.getZ(), max.getZ()));

        // Calculate distance from player to closest point
        double distanceSquared = Math.pow(playerLoc.getX() - closestX, 2) +
                Math.pow(playerLoc.getY() - closestY, 2) +
                Math.pow(playerLoc.getZ() - closestZ, 2);

        return distanceSquared <= (distance * distance);
    }

    private Set<Location> getRegionBorderBlocks(ProtectedRegion region, Location playerLoc) {
        Set<Location> blocks = new HashSet<>();

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();

        // Create border outline (edges only, not full walls)
        // This creates a wireframe effect with glass panes

        // Bottom and top horizontal edges
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Only add corners and edges, not the entire face
                if (x == minX || x == maxX || z == minZ || z == maxZ) {
                    blocks.add(new Location(playerLoc.getWorld(), x, minY, z));
                    blocks.add(new Location(playerLoc.getWorld(), x, maxY, z));
                }
            }
        }

        // Vertical edges
        for (int y = minY + 1; y < maxY; y++) {
            // Four vertical edges
            blocks.add(new Location(playerLoc.getWorld(), minX, y, minZ));
            blocks.add(new Location(playerLoc.getWorld(), minX, y, maxZ));
            blocks.add(new Location(playerLoc.getWorld(), maxX, y, minZ));
            blocks.add(new Location(playerLoc.getWorld(), maxX, y, maxZ));
        }

        // Add middle horizontal lines for visibility (every 4 blocks)
        for (int x = minX; x <= maxX; x += 4) {
            for (int y = minY; y <= maxY; y++) {
                blocks.add(new Location(playerLoc.getWorld(), x, y, minZ));
                blocks.add(new Location(playerLoc.getWorld(), x, y, maxZ));
            }
        }

        for (int z = minZ; z <= maxZ; z += 4) {
            for (int y = minY; y <= maxY; y++) {
                blocks.add(new Location(playerLoc.getWorld(), minX, y, z));
                blocks.add(new Location(playerLoc.getWorld(), maxX, y, z));
            }
        }

        return blocks;
    }
}