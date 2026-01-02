package net.saturn.limiter.listener;

import net.saturn.BetterCombatLogging;
import net.saturn.limiter.manager.ItemLimitManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemLimitListener implements Listener {

    private final BetterCombatLogging plugin;
    private final ItemLimitManager itemLimitManager;

    public ItemLimitListener(BetterCombatLogging plugin, ItemLimitManager itemLimitManager) {
        this.plugin = plugin;
        this.itemLimitManager = itemLimitManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        Material material = item.getType();

        if (!itemLimitManager.isItemLimited(material)) {
            return;
        }

        Integer limit = itemLimitManager.getLimit(material);

        // If completely banned (limit = 0)
        if (limit == 0) {
            event.setCancelled(true);

            String message = plugin.getConfig().getString(
                    "messages.item-blocked-pickup-banned",
                    "&cYou cannot pick up &e{item}&c - it is banned!"
            ).replace("{item}", formatItemName(item));
            player.sendMessage(colorize(message));
            return;
        }

        // Check if picking up would exceed limit
        int currentCount = itemLimitManager.countItemInInventory(player, material);
        int pickupAmount = item.getAmount();

        if (currentCount >= limit) {
            // Already at or over limit
            event.setCancelled(true);

            String message = plugin.getConfig().getString(
                            "messages.item-blocked-pickup-limit",
                            "&cYou cannot pick up &e{item}&c - you already have the maximum ({limit})!"
                    ).replace("{item}", formatItemName(item))
                    .replace("{limit}", String.valueOf(limit));
            player.sendMessage(colorize(message));
        } else if (currentCount + pickupAmount > limit) {
            // Pickup would exceed limit - allow partial pickup
            int canPickup = limit - currentCount;

            event.getItem().getItemStack().setAmount(pickupAmount - canPickup);

            // Add only what they can carry
            ItemStack toAdd = item.clone();
            toAdd.setAmount(canPickup);
            player.getInventory().addItem(toAdd);

            event.setCancelled(true);

            String message = plugin.getConfig().getString(
                            "messages.item-partial-pickup",
                            "&eYou can only pick up &6{amount} &e{item} (limit: {limit})"
                    ).replace("{item}", formatItemName(item))
                    .replace("{amount}", String.valueOf(canPickup))
                    .replace("{limit}", String.valueOf(limit));
            player.sendMessage(colorize(message));
        }
        // else: pickup is fine, let it proceed
    }

    @EventHandler(priority = EventPriority.LOWEST) // Use LOWEST to catch it first
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Don't check creative inventory
        if (event.getInventory().getType() == InventoryType.CREATIVE) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();

        // Only check if there's actually an item being clicked
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Material material = clickedItem.getType();

        if (!itemLimitManager.isItemLimited(material)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();

        // If clicked inventory is null, ignore
        if (clickedInventory == null) {
            return;
        }

        // Check if player is taking item FROM a non-player inventory (chest, etc.)
        // This means they're trying to move it INTO their inventory
        boolean takingFromContainer = !clickedInventory.equals(player.getInventory());

        if (!takingFromContainer) {
            // Player is clicking in their own inventory - allow (they might be dropping/moving around)
            return;
        }

        // At this point, player is trying to take a limited item from a container
        Integer limit = itemLimitManager.getLimit(material);

        // If completely banned
        if (limit == 0) {
            event.setCancelled(true);

            String message = plugin.getConfig().getString(
                    "messages.item-blocked-take",
                    "&cYou cannot take &e{item}&c - it is banned!"
            ).replace("{item}", formatItemName(clickedItem));
            player.sendMessage(colorize(message));
            return;
        }

        // Check current count
        int currentCount = itemLimitManager.countItemInInventory(player, material);
        int itemAmount = clickedItem.getAmount();

        // Already at limit
        if (currentCount >= limit) {
            event.setCancelled(true);

            String message = plugin.getConfig().getString(
                            "messages.item-blocked-take-limit",
                            "&cYou cannot take &e{item}&c - you already have the maximum ({limit})!"
                    ).replace("{item}", formatItemName(clickedItem))
                    .replace("{limit}", String.valueOf(limit));
            player.sendMessage(colorize(message));
            return;
        }

        // Would exceed limit
        if (currentCount + itemAmount > limit) {
            event.setCancelled(true);

            int canTake = limit - currentCount;
            String message = plugin.getConfig().getString(
                            "messages.item-blocked-take-partial",
                            "&cYou can only take &6{amount} &cmore &e{item}&c (limit: {limit})"
                    ).replace("{item}", formatItemName(clickedItem))
                    .replace("{amount}", String.valueOf(canTake))
                    .replace("{limit}", String.valueOf(limit));
            player.sendMessage(colorize(message));
            return;
        }

        // Within limit - allow the action
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check and enforce limits after join
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    enforceAllLimits(player);
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay
    }

    /**
     * Enforces all item limits for a player (only on login)
     */
    private void enforceAllLimits(Player player) {
        if (!itemLimitManager.hasLimitedItems()) {
            return;
        }

        int totalRemoved = 0;

        for (Material material : itemLimitManager.getLimitedItems().keySet()) {
            int removed = itemLimitManager.enforceLimit(player, material);
            totalRemoved += removed;
        }

        if (totalRemoved > 0) {
            String message = plugin.getConfig().getString(
                    "messages.items-removed-login",
                    "&cRemoved &e{count} &climited items from your inventory!"
            ).replace("{count}", String.valueOf(totalRemoved));
            player.sendMessage(colorize(message));
        }
    }

    private String formatItemName(ItemStack item) {
        return formatMaterialName(item.getType());
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            formatted.append(word.substring(0, 1).toUpperCase())
                    .append(word.substring(1).toLowerCase());
        }

        return formatted.toString();
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}