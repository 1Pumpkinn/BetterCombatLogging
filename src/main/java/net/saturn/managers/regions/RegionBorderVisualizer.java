package net.saturn.managers.regions;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
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
    private final Map<UUID, Long> lastUpdate;
    private BukkitRunnable updateTask;
    private static final long UPDATE_INTERVAL_MS = 500; // Update every 500ms per player

    public RegionBorderVisualizer(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.playerVisibleBlocks = new HashMap<>();
        this.lastUpdate = new HashMap<>();
    }

    public void start() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    long lastUpdateTime = lastUpdate.getOrDefault(uuid, 0L);

                    // Only update if enough time has passed since last update
                    if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
                        updatePlayerView(player);
                        lastUpdate.put(uuid, currentTime);
                    }
                }
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 5L); // Run every 5 ticks (0.25s)
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
        lastUpdate.clear();
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
                    int distance = plugin.getConfig().getInt("region-visualizer.distance", 15);

                    for (String regionName : blockedRegions) {
                        ProtectedRegion region = regionManager.getRegion(regionName);
                        if (region != null && isPlayerNearRegion(player.getLocation(), region, distance)) {
                            newBlocks.addAll(getRegionBorderBlocks(region, player.getLocation()));
                        }
                    }
                }
            }
        }

        // Batch send block changes for better performance
        List<Location> toRemove = new ArrayList<>();
        List<Location> toAdd = new ArrayList<>();

        // Find blocks to remove
        for (Location loc : oldBlocks) {
            if (!newBlocks.contains(loc)) {
                toRemove.add(loc);
            }
        }

        // Find blocks to add
        for (Location loc : newBlocks) {
            if (!oldBlocks.contains(loc)) {
                toAdd.add(loc);
            }
        }

        // Send changes in batches
        sendBlockChanges(player, toRemove, false);
        sendBlockChanges(player, toAdd, true);

        playerVisibleBlocks.put(uuid, newBlocks);
    }

    private void sendBlockChanges(Player player, List<Location> locations, boolean isGlass) {
        if (locations.isEmpty()) {
            return;
        }

        // Process in smaller batches to avoid overwhelming client
        int batchSize = 50;
        for (int i = 0; i < locations.size(); i += batchSize) {
            int end = Math.min(i + batchSize, locations.size());
            List<Location> batch = locations.subList(i, end);

            for (Location loc : batch) {
                if (isGlass) {
                    player.sendBlockChange(loc, Material.RED_STAINED_GLASS.createBlockData());
                } else {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }
        }
    }

    public void clearPlayerView(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Location> blocks = playerVisibleBlocks.remove(uuid);
        lastUpdate.remove(uuid);

        if (blocks != null && !blocks.isEmpty()) {
            List<Location> blockList = new ArrayList<>(blocks);
            sendBlockChanges(player, blockList, false);
        }
    }

    public boolean isVisualizerBlock(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        Set<Location> blocks = playerVisibleBlocks.get(uuid);
        return blocks != null && blocks.contains(location);
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

        int playerY = playerLoc.getBlockY();
        int verticalRange = plugin.getConfig().getInt("region-visualizer.vertical-range", 5);

        // Calculate the Y range to show (only show blocks near player's height)
        int visualMinY = Math.max(minY + 1, playerY - verticalRange);
        int visualMaxY = Math.min(maxY, playerY + verticalRange);

        // Ensure we always show at least ground level
        visualMinY = Math.max(minY + 1, visualMinY);

        // Render all blocks with no spacing to prevent glitches (wind charge float exploit)
        // North wall (minZ)
        for (int x = minX; x <= maxX; x++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                Location loc = new Location(playerLoc.getWorld(), x, y, minZ);
                if (loc.getBlock().getType() == Material.AIR) {
                    blocks.add(loc);
                }
            }
        }

        // South wall (maxZ)
        for (int x = minX; x <= maxX; x++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                Location loc = new Location(playerLoc.getWorld(), x, y, maxZ);
                if (loc.getBlock().getType() == Material.AIR) {
                    blocks.add(loc);
                }
            }
        }

        // West wall (minX)
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                Location loc = new Location(playerLoc.getWorld(), minX, y, z);
                if (loc.getBlock().getType() == Material.AIR) {
                    blocks.add(loc);
                }
            }
        }

        // East wall (maxX)
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                Location loc = new Location(playerLoc.getWorld(), maxX, y, z);
                if (loc.getBlock().getType() == Material.AIR) {
                    blocks.add(loc);
                }
            }
        }

        return blocks;
    }
}