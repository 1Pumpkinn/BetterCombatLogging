package net.saturn.tasks.cleanup;

import net.saturn.BetterCombatLogging;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class MapCleanupTask extends BukkitRunnable {

    private final BetterCombatLogging plugin;
    private final Set<Material> blocksToRemove;
    private static final int CHUNKS_PER_TICK = 10; // Process chunks gradually

    public MapCleanupTask(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.blocksToRemove = new HashSet<>();

        // Add materials to remove
        // Blocks
        blocksToRemove.add(Material.PALE_OAK_LOG);
        blocksToRemove.add(Material.PALE_OAK_PLANKS);
        blocksToRemove.add(Material.PALE_OAK_STAIRS);
        blocksToRemove.add(Material.PALE_OAK_SLAB);
        blocksToRemove.add(Material.PALE_OAK_FENCE);
        blocksToRemove.add(Material.PALE_OAK_FENCE_GATE);
        blocksToRemove.add(Material.PALE_OAK_SIGN);
        blocksToRemove.add(Material.PALE_OAK_TRAPDOOR);
        blocksToRemove.add(Material.PALE_OAK_PRESSURE_PLATE);
        blocksToRemove.add(Material.PALE_OAK_BUTTON);

        blocksToRemove.add(Material.CRIMSON_HYPHAE);
        blocksToRemove.add(Material.CRIMSON_PLANKS);
        blocksToRemove.add(Material.CRIMSON_STAIRS);
        blocksToRemove.add(Material.CRIMSON_PRESSURE_PLATE);
        blocksToRemove.add(Material.CRIMSON_FENCE);
        blocksToRemove.add(Material.CRIMSON_FENCE_GATE);
        blocksToRemove.add(Material.CRIMSON_SIGN);
        blocksToRemove.add(Material.CRIMSON_TRAPDOOR);
        blocksToRemove.add(Material.CRIMSON_DOOR);
        blocksToRemove.add(Material.CRIMSON_BUTTON);

        blocksToRemove.add(Material.COBBLESTONE);
        blocksToRemove.add(Material.COBBLESTONE_STAIRS);
        blocksToRemove.add(Material.COBBLESTONE_SLAB);
        blocksToRemove.add(Material.COBBLESTONE_WALL);

        // Ores
        blocksToRemove.add(Material.IRON_BLOCK);
        blocksToRemove.add(Material.GOLD_BLOCK);
        blocksToRemove.add(Material.COAL_BLOCK);
        blocksToRemove.add(Material.REDSTONE_BLOCK);
        blocksToRemove.add(Material.EMERALD_BLOCK);
        blocksToRemove.add(Material.LAPIS_BLOCK);
        blocksToRemove.add(Material.COPPER_BLOCK);
        blocksToRemove.add(Material.EXPOSED_COPPER);
        blocksToRemove.add(Material.WEATHERED_COPPER);
        blocksToRemove.add(Material.OXIDIZED_COPPER);
        blocksToRemove.add(Material.DIAMOND_BLOCK);
        blocksToRemove.add(Material.NETHERITE_BLOCK);
        blocksToRemove.add(Material.ANCIENT_DEBRIS);

        // Util
        blocksToRemove.add(Material.COBWEB);
        blocksToRemove.add(Material.REDSTONE);

        // Fluids
        blocksToRemove.add(Material.WATER);
        blocksToRemove.add(Material.LAVA);
    }

    @Override
    public void run() {
        // Start async cleanup for all worlds
        for (World world : Bukkit.getWorlds()) {
            if (!plugin.getConfig().getBoolean("cleanup.worlds." + world.getName() + ".enabled", true)) {
                continue;
            }
            startAsyncCleanup(world);
        }
    }

    private void startAsyncCleanup(World world) {
        WorldBorder border = world.getWorldBorder();
        double borderSize = border.getSize();
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        // Calculate chunk boundaries within world border
        int minChunkX = (int) Math.floor((centerX - borderSize / 2) / 16);
        int maxChunkX = (int) Math.ceil((centerX + borderSize / 2) / 16);
        int minChunkZ = (int) Math.floor((centerZ - borderSize / 2) / 16);
        int maxChunkZ = (int) Math.ceil((centerZ + borderSize / 2) / 16);

        // Process chunks in batches to avoid lag
        new BukkitRunnable() {
            private int currentChunkX = minChunkX;
            private int currentChunkZ = minChunkZ;
            private int totalBlocksCleared = 0;

            @Override
            public void run() {
                int chunksProcessed = 0;

                while (chunksProcessed < CHUNKS_PER_TICK && currentChunkX <= maxChunkX) {
                    if (currentChunkZ > maxChunkZ) {
                        currentChunkZ = minChunkZ;
                        currentChunkX++;
                        continue;
                    }

                    // Only process loaded chunks to avoid forcing chunk loads
                    if (world.isChunkLoaded(currentChunkX, currentChunkZ)) {
                        totalBlocksCleared += clearBlocksInChunk(world, currentChunkX, currentChunkZ);
                    }

                    currentChunkZ++;
                    chunksProcessed++;
                }

                // Check if done
                if (currentChunkX > maxChunkX) {
                    cancel();
                    onCleanupComplete(world, totalBlocksCleared);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Process every tick
    }

    private int clearBlocksInChunk(World world, int chunkX, int chunkZ) {
        int count = 0;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        // Iterate through blocks in chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = world.getBlockAt(chunkX * 16 + x, y, chunkZ * 16 + z);

                    if (blocksToRemove.contains(block.getType())) {
                        block.setType(Material.AIR);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private void onCleanupComplete(World world, int blocksCleared) {
        // Broadcast cleanup message if enabled
        if (plugin.getConfig().getBoolean("cleanup.broadcast-cleanup", true) && blocksCleared > 0) {
            String message = plugin.getConfig().getString(
                            "messages.cleanup-complete",
                            "&a✔ Map cleanup completed in {world}! Removed {count} blocks."
                    ).replace("{count}", String.valueOf(blocksCleared))
                    .replace("{world}", world.getName());

            Bukkit.broadcastMessage(colorize(message));
        }

        plugin.getLogger().info("Map cleanup completed in " + world.getName() + ". Removed " + blocksCleared + " blocks.");
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}