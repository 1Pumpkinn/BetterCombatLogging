package net.saturn.listeners.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class RegionBlockBreakListener implements Listener {

    private final BetterCombatLogging plugin;

    public RegionBlockBreakListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        // Prevent breaking blocks at visualizer locations
        if (plugin.getRegionVisualizer() != null) {
            if (plugin.getRegionVisualizer().isVisualizerBlock(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                // Refresh the fake block
                event.getPlayer().sendBlockChange(
                        event.getBlock().getLocation(),
                        Material.RED_STAINED_GLASS.createBlockData()
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Prevent placing blocks at visualizer locations
        if (plugin.getRegionVisualizer() != null) {
            if (plugin.getRegionVisualizer().isVisualizerBlock(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                // Refresh the fake block
                event.getPlayer().sendBlockChange(
                        event.getBlock().getLocation(),
                        Material.RED_STAINED_GLASS.createBlockData()
                );
            }
        }
    }
}