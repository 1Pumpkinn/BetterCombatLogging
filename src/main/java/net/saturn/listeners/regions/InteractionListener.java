package net.saturn.listeners.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractionListener implements Listener {

    private final BetterCombatLogging plugin;

    public InteractionListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Prevent interaction with visualizer blocks
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && plugin.getRegionVisualizer() != null) {
                if (plugin.getRegionVisualizer().isVisualizerBlock(event.getPlayer(), event.getClickedBlock().getLocation())) {
                    event.setCancelled(true);
                    // Refresh the fake block immediately
                    event.getPlayer().sendBlockChange(
                            event.getClickedBlock().getLocation(),
                            Material.RED_STAINED_GLASS.createBlockData()
                    );
                }
            }
        }
    }
}