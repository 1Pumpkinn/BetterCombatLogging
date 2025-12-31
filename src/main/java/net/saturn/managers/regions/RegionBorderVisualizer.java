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

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            clearPlayerView(player);
        }
        playerVisibleBlocks.clear();
        lastUpdate.clear();
    }

    private void updatePlayerView(Player player) {
        UUID uuid = player.getUniqueId();
        Set<Location> oldBlocks = playerVisibleBlocks.getOrDefault(uuid, new HashSet<>());
        Set<Location> newBlocks = new HashSet<>();

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

        // Determine which blocks to add/remove
        List<Location> toRemove = new ArrayList<>();
        List<Location> toAdd = new ArrayList<>();

        for (Location loc : oldBlocks) if (!newBlocks.contains(loc)) toRemove.add(loc);
        for (Location loc : newBlocks) if (!oldBlocks.contains(loc)) toAdd.add(loc);

        // Send changes
        sendBlockChanges(player, toRemove, false);
        sendBlockChanges(player, toAdd, true);

        // Store new set
        playerVisibleBlocks.put(uuid, newBlocks);
    }

    private void sendBlockChanges(Player player, List<Location> locations, boolean isGlass) {
        if (locations.isEmpty()) return;
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
        if (blocks != null && !blocks.isEmpty()) sendBlockChanges(player, new ArrayList<>(blocks), false);
    }

    public boolean isVisualizerBlock(Player player, Location location) {
        UUID uuid = player.getUniqueId();
        Set<Location> blocks = playerVisibleBlocks.get(uuid);
        return blocks != null && blocks.contains(location);
    }

    public void refresh(Player player) {
        Set<Location> blocks = playerVisibleBlocks.get(player.getUniqueId());
        if (blocks == null) return;
        for (Location loc : blocks) player.sendBlockChange(loc, Material.RED_STAINED_GLASS.createBlockData());
    }

    private boolean isPlayerNearRegion(Location playerLoc, ProtectedRegion region, int distance) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        double closestX = Math.max(min.getX(), Math.min(playerLoc.getX(), max.getX()));
        double closestY = Math.max(min.getY(), Math.min(playerLoc.getY(), max.getY()));
        double closestZ = Math.max(min.getZ(), Math.min(playerLoc.getZ(), max.getZ()));

        double distanceSquared = Math.pow(playerLoc.getX() - closestX, 2) +
                Math.pow(playerLoc.getY() - closestY, 2) +
                Math.pow(playerLoc.getZ() - closestZ, 2);

        return distanceSquared <= (distance * distance);
    }

    private Set<Location> getRegionBorderBlocks(ProtectedRegion region, Location playerLoc) {
        Set<Location> blocks = new HashSet<>();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int minX = min.getX(), minY = min.getY(), minZ = min.getZ();
        int maxX = max.getX(), maxY = max.getY(), maxZ = max.getZ();
        int playerY = playerLoc.getBlockY();
        int verticalRange = plugin.getConfig().getInt("region-visualizer.vertical-range", 5);
        int visualMinY = Math.max(minY + 1, playerY - verticalRange);
        int visualMaxY = Math.min(maxY, playerY + verticalRange);

        // North/South walls
        for (int x = minX; x <= maxX; x++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                blocks.add(new Location(playerLoc.getWorld(), x, y, minZ));
                blocks.add(new Location(playerLoc.getWorld(), x, y, maxZ));
            }
        }

        // West/East walls
        for (int z = minZ; z <= maxZ; z++) {
            for (int y = visualMinY; y <= visualMaxY; y++) {
                blocks.add(new Location(playerLoc.getWorld(), minX, y, z));
                blocks.add(new Location(playerLoc.getWorld(), maxX, y, z));
            }
        }

        return blocks;
    }
}
