package net.saturn.listeners.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class RegionVisualizerListener implements Listener {

    private final BetterCombatLogging plugin;

    public RegionVisualizerListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clear fake blocks when player disconnects
        if (plugin.getRegionVisualizer() != null) {
            plugin.getRegionVisualizer().clearPlayerView(event.getPlayer());
        }
    }
}