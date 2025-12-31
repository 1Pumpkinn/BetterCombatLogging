package net.saturn.tasks.cleanup;

import net.saturn.BetterCombatLogging;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class MapCleanupTask extends BukkitRunnable {

    private final BetterCombatLogging plugin;
    private final Set<Material> blocksToRemove;

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
        blocksToRemove.add(Material.PALE_OAK_SHELF);
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

        blocksToRemove.add(Material.COBBLESTONE );
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

        // Fluids
        blocksToRemove.add(Material.WATER);
        blocksToRemove.add(Material.LAVA);

        // Entities
        blocksToRemove.add(Material.PALE_OAK_BOAT);
        blocksToRemove.add(Material.PALE_OAK_CHEST_BOAT);
    }

    @Override
    public void run() {
        int blocksCleared = 0;

        // Get all worlds
        for (World world : Bukkit.getWorlds()) {
            // Skip if world cleanup is disabled
            if (!plugin.getConfig().getBoolean("cleanup.worlds." + world.getName() + ".enabled", true)) {
                continue;
            }

            // Clear blocks in all loaded chunks
            for (Chunk chunk : world.getLoadedChunks()) {
                blocksCleared += clearBlocksInChunk(chunk);
            }
        }

        // Broadcast cleanup message if enabled
        if (plugin.getConfig().getBoolean("cleanup.broadcast-cleanup", true) && blocksCleared > 0) {
            String message = plugin.getConfig().getString(
                    "messages.cleanup-complete",
                    "&a✔ Map cleanup completed! Removed {count} blocks."
            ).replace("{count}", String.valueOf(blocksCleared));

            Bukkit.broadcastMessage(colorize(message));
        }

        plugin.getLogger().info("Map cleanup completed. Removed " + blocksCleared + " blocks.");
    }

    private int clearBlocksInChunk(Chunk chunk) {
        int count = 0;

        // Iterate through all blocks in the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (blocksToRemove.contains(block.getType())) {
                        block.setType(Material.AIR);
                        count++;
                    }
                }
            }
        }

        return count;
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}