package net.saturn.listeners.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractionListener implements Listener {

    private final BetterCombatLogging plugin;

    // Cooldown to prevent packet spam
    private final Map<UUID, Long> refreshCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 150;

    public InteractionListener(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (plugin.getRegionVisualizer() == null) return;

        Player player = event.getPlayer();
        Location clicked = event.getClickedBlock().getLocation();

        // Only respond if block is a visualizer
        if (!plugin.getRegionVisualizer().isVisualizerBlock(player, clicked)) return;

        event.setCancelled(true);

        // Resend all visualizer blocks (with cooldown)
        long now = System.currentTimeMillis();
        long last = refreshCooldown.getOrDefault(player.getUniqueId(), 0L);

        if (now - last >= COOLDOWN_MS) {
            plugin.getRegionVisualizer().refresh(player);
            refreshCooldown.put(player.getUniqueId(), now);
        }

        // Optional: fling player if in combat
        if (plugin.getCombatManager().isInCombat(player)) {
            flingPlayer(player, clicked);
            player.sendMessage(colorize("&cYou cannot interact with blocked regions while in combat!"));
        }
    }

    private void flingPlayer(Player player, Location blockLocation) {
        Vector direction = player.getLocation().toVector().subtract(blockLocation.toVector()).normalize();
        direction.setY(0.5);
        direction.multiply(1.5);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2f);
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}
