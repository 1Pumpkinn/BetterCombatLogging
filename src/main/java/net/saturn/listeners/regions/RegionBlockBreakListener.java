package net.saturn.listeners.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RegionBlockBreakListener implements Listener {

    private final BetterCombatLogging plugin;

    public RegionBlockBreakListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDamage(BlockDamageEvent event) {
        // Prevent even starting to break visualizer blocks
        if (plugin.getRegionVisualizer() != null) {
            Player player = event.getPlayer();
            Location blockLocation = event.getBlock().getLocation();

            if (plugin.getRegionVisualizer().isVisualizerBlock(player, blockLocation)) {
                event.setCancelled(true);

                // Refresh the block immediately
                player.sendBlockChange(blockLocation, Material.RED_STAINED_GLASS.createBlockData());

                // Fling player if in combat
                if (plugin.getCombatManager().isInCombat(player)) {
                    flingPlayerAway(player, blockLocation);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent breaking blocks at visualizer locations
        if (plugin.getRegionVisualizer() != null) {
            Player player = event.getPlayer();
            Location blockLocation = event.getBlock().getLocation();

            if (plugin.getRegionVisualizer().isVisualizerBlock(player, blockLocation)) {
                event.setCancelled(true);

                // Refresh the fake block multiple times to ensure it stays
                for (int i = 0; i < 3; i++) {
                    final int delay = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() && plugin.getRegionVisualizer() != null) {
                                player.sendBlockChange(blockLocation, Material.RED_STAINED_GLASS.createBlockData());
                            }
                        }
                    }.runTaskLater(plugin, delay * 2L);
                }

                // Fling player if in combat
                if (plugin.getCombatManager().isInCombat(player)) {
                    flingPlayerAway(player, blockLocation);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Prevent placing blocks at visualizer locations
        if (plugin.getRegionVisualizer() != null) {
            Player player = event.getPlayer();
            Location blockLocation = event.getBlock().getLocation();

            if (plugin.getRegionVisualizer().isVisualizerBlock(player, blockLocation)) {
                event.setCancelled(true);

                // Refresh the fake block
                player.sendBlockChange(blockLocation, Material.RED_STAINED_GLASS.createBlockData());

                // Fling player if in combat
                if (plugin.getCombatManager().isInCombat(player)) {
                    flingPlayerAway(player, blockLocation);
                }
            }
        }
    }

    private void flingPlayerAway(Player player, Location blockLocation) {
        // Calculate direction away from the block
        Vector direction = player.getLocation().toVector().subtract(blockLocation.toVector()).normalize();

        // Set Y component to launch upward
        direction.setY(0.5);

        // Increase horizontal strength
        direction.multiply(1.5);

        // Apply velocity
        player.setVelocity(direction);
    }
}