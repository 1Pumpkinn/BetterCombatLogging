package net.saturn.limiter.listener;

import net.saturn.BetterCombatLogging;
import net.saturn.limiter.manager.ItemLimitManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
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

    /* ============================================================
       PICKUP (GROUND → INVENTORY)
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem().getItemStack();
        Material material = stack.getType();

        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        // If banned or would exceed limit, cancel pickup
        if (limit == 0) {
            event.setCancelled(true);
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                                    "messages.item-blocked-pickup-banned",
                                    "&cYou cannot pick up &e{item}&c - it is banned!"
                            )
                            .replace("{item}", format(material))
            ));
        } else if (current >= limit) {
            event.setCancelled(true);
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                                    "messages.item-blocked-pickup-limit",
                                    "&cYou cannot pick up &e{item}&c - you are at the maximum ({limit})!"
                            )
                            .replace("{item}", format(material))
                            .replace("{limit}", String.valueOf(limit))
            ));
        } else if (current + stack.getAmount() > limit) {
            event.setCancelled(true);
            int canPickup = limit - current;
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                                    "messages.item-partial-pickup",
                                    "&cYou can only pick up &6{amount} &cmore &e{item}&c (limit: {limit})"
                            )
                            .replace("{item}", format(material))
                            .replace("{amount}", String.valueOf(canPickup))
                            .replace("{limit}", String.valueOf(limit))
            ));
        }
    }

    /* ============================================================
       INVENTORY CLICK - HANDLE ALL CASES INCLUDING SHIFT-CLICK
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() == InventoryType.CREATIVE) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        Inventory playerInv = player.getInventory();
        InventoryAction action = event.getAction();

        // Special handling for hotbar swaps (number keys)
        if (action == InventoryAction.HOTBAR_SWAP || action == InventoryAction.HOTBAR_MOVE_AND_READD) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton < 9) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                ItemStack clickedItem = event.getCurrentItem();

                // Check if we're swapping FROM a container TO player hotbar
                if (clicked != playerInv && clickedItem != null && clickedItem.getType() != Material.AIR) {
                    Material material = clickedItem.getType();

                    if (itemLimitManager.isItemLimited(material)) {
                        int limit = itemLimitManager.getLimit(material);
                        int current = itemLimitManager.countItemInInventory(player, material);

                        // If banned
                        if (limit == 0) {
                            event.setCancelled(true);
                            player.updateInventory();
                            player.sendMessage(colorize(
                                    plugin.getConfig().getString(
                                                    "messages.item-blocked-place-banned",
                                                    "&cYou cannot have &e{item}&c - it is banned!"
                                            )
                                            .replace("{item}", format(material))
                            ));
                            return;
                        }

                        // If at or over limit, cancel
                        if (current >= limit) {
                            event.setCancelled(true);
                            player.updateInventory();
                            player.sendMessage(colorize(
                                    plugin.getConfig().getString(
                                                    "messages.item-blocked-place-limit",
                                                    "&cYou already have the maximum &e{item}&c ({limit})!"
                                            )
                                            .replace("{item}", format(material))
                                            .replace("{limit}", String.valueOf(limit))
                            ));
                            return;
                        }
                    }
                }
            }
            return;
        }

        // Get the item being moved
        ItemStack movingItem = null;
        Material material = null;

        // Determine what item is being moved and where
        switch (action) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
                // Taking item from cursor and placing in inventory
                movingItem = event.getCursor();
                break;

            case MOVE_TO_OTHER_INVENTORY:
                // Shift-clicking to move items
                movingItem = event.getCurrentItem();
                break;

            case SWAP_WITH_CURSOR:
                // Swapping cursor item with clicked item
                movingItem = event.getCursor();
                break;

            case COLLECT_TO_CURSOR:
                // Double-clicking to collect items
                movingItem = event.getCursor();
                break;

            default:
                // Allow all other actions (PICKUP, DROP, etc.)
                return;
        }

        if (movingItem == null || movingItem.getType() == Material.AIR) return;
        material = movingItem.getType();

        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);

        // If item is banned, always cancel
        if (limit == 0) {
            // Only cancel if trying to add to player inventory
            if (isAddingToPlayerInventory(action, clicked, playerInv)) {
                event.setCancelled(true);
                player.updateInventory();
                player.sendMessage(colorize(
                        plugin.getConfig().getString(
                                        "messages.item-blocked-place-banned",
                                        "&cYou cannot have &e{item}&c - it is banned!"
                                )
                                .replace("{item}", format(material))
                ));
            }
            return;
        }

        // Check if this action would add items to player inventory
        if (isAddingToPlayerInventory(action, clicked, playerInv)) {
            int current = itemLimitManager.countItemInInventory(player, material);

            // If already at or over limit, cancel
            if (current >= limit) {
                event.setCancelled(true);
                player.updateInventory();
                player.sendMessage(colorize(
                        plugin.getConfig().getString(
                                        "messages.item-blocked-place-limit",
                                        "&cYou already have the maximum &e{item}&c ({limit})!"
                                )
                                .replace("{item}", format(material))
                                .replace("{limit}", String.valueOf(limit))
                ));
            }
        }
    }

    /**
     * Determines if an inventory action is adding items to the player's inventory
     */
    private boolean isAddingToPlayerInventory(InventoryAction action, Inventory clicked, Inventory playerInv) {
        switch (action) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
            case COLLECT_TO_CURSOR:
                // These add to player inventory if clicking in player inventory
                return clicked == playerInv;

            case MOVE_TO_OTHER_INVENTORY:
                // Shift-click adds to player inventory if clicking in a container
                return clicked != playerInv;

            default:
                return false;
        }
    }

    /* ============================================================
       ESC / INVENTORY CLOSE FIX
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        Material material = cursor.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        // If banned or would exceed limit, drop the cursor item
        if (limit == 0 || current + cursor.getAmount() > limit) {
            player.setItemOnCursor(null);
            player.getWorld().dropItemNaturally(player.getLocation(), cursor);

            if (limit == 0) {
                player.sendMessage(colorize(
                        plugin.getConfig().getString(
                                        "messages.item-blocked-place-banned",
                                        "&cYou cannot have &e{item}&c - it is banned!"
                                )
                                .replace("{item}", format(material))
                ));
            } else {
                player.sendMessage(colorize(
                        plugin.getConfig().getString(
                                        "messages.item-blocked-place-limit",
                                        "&cYou already have the maximum &e{item}&c ({limit})!"
                                )
                                .replace("{item}", format(material))
                                .replace("{limit}", String.valueOf(limit))
                ));
            }
        }
    }

    /* ============================================================
       DROP WHILE HOLDING CURSOR FIX
       ============================================================ */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        Material material = cursor.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        // If banned or would exceed limit, drop the cursor item
        if (limit == 0 || current + cursor.getAmount() > limit) {
            player.setItemOnCursor(null);
            player.getWorld().dropItemNaturally(player.getLocation(), cursor);
        }
    }

    /* ============================================================
       LOGIN ENFORCEMENT (ONLY PLACE ITEMS ARE REMOVED)
       ============================================================ */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                int removed = 0;
                for (Material mat : itemLimitManager.getLimitedItems().keySet()) {
                    removed += itemLimitManager.enforceLimit(player, mat);
                }

                if (removed > 0) {
                    player.sendMessage(colorize(
                            plugin.getConfig().getString(
                                    "messages.items-removed-login",
                                    "&cRemoved &e{count} &climited items from your inventory!"
                            ).replace("{count}", String.valueOf(removed))
                    ));
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    /* ============================================================
       UTIL
       ============================================================ */
    private String format(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            out.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1))
                    .append(" ");
        }
        return out.toString().trim();
    }

    private String colorize(String msg) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', msg);
    }
}