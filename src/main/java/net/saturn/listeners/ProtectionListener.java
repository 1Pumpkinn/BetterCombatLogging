package net.saturn.betterCombatLogger.listeners;

import net.saturn.betterCombatLogger.BetterCombatLogger;
import net.saturn.betterCombatLogger.managers.ProtectionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ProtectionListener implements Listener {

    private final BetterCombatLogger plugin;
    private final ProtectionManager protectionManager;

    public ProtectionListener(BetterCombatLogger plugin, ProtectionManager protectionManager) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Don't enforce in enchanting tables or anvils
        if (event.getInventory().getType() == InventoryType.ENCHANTING ||
                event.getInventory().getType() == InventoryType.ANVIL) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }

        // Check and enforce limit after a small delay to ensure the item is in inventory
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    checkPlayerInventory(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check inventory after join
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    checkPlayerInventory(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();

        // Check the picked up item
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    boolean modified = protectionManager.enforceLimit(item);
                    if (modified) {
                        player.sendMessage(colorize(plugin.getConfig().getString(
                                "messages.protection-limited",
                                "&eYour item's Protection enchantment has been limited to the maximum allowed level."
                        )));
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    /**
     * Checks and enforces protection limits on all items in player's inventory
     */
    private void checkPlayerInventory(Player player) {
        if (!protectionManager.hasLimit()) {
            return; // No limit set
        }

        boolean modified = false;

        // Check main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (protectionManager.enforceLimit(item)) {
                modified = true;
            }
        }

        // Check armor
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (protectionManager.enforceLimit(item)) {
                modified = true;
            }
        }

        // Check off-hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (protectionManager.enforceLimit(offHand)) {
            modified = true;
        }

        if (modified) {
            player.sendMessage(colorize(plugin.getConfig().getString(
                    "messages.protection-limited",
                    "&eYour items' Protection enchantments have been limited to the maximum allowed level."
            )));
        }
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}