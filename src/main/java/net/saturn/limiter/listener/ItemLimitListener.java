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

    /* ============================================================
       ITEM PICKUP (GROUND → INVENTORY)
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem().getItemStack();
        Material material = stack.getType();

        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);

        // Fully banned
        if (limit == 0) {
            event.setCancelled(true);
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                            "messages.item-blocked-pickup-banned",
                            "&cYou cannot pick up &e{item}&c - it is banned!"
                    ).replace("{item}", format(material))
            ));
            return;
        }

        int current = itemLimitManager.countItemInInventory(player, material);

        // Would increase inventory beyond limit
        if (current + stack.getAmount() > limit) {
            event.setCancelled(true);
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                                    "messages.item-blocked-pickup-limit",
                                    "&cYou already have the maximum &e{item}&c ({limit})!"
                            )
                            .replace("{item}", format(material))
                            .replace("{limit}", String.valueOf(limit))
            ));
        }
    }

    /* ============================================================
       INVENTORY INTERACTIONS
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Ignore creative
        if (event.getInventory().getType() == InventoryType.CREATIVE) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        Inventory playerInv = player.getInventory();
        InventoryAction action = event.getAction();

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        /* ------------------------------------------------------------
           ALWAYS ALLOW REMOVING ITEMS FROM CONTAINERS
           ------------------------------------------------------------ */
        if (clicked != playerInv) {
            // Taking from chest/furnace/shulker/etc.
            return;
        }

        /* ------------------------------------------------------------
           DROPPING ITEMS (Q / CTRL+Q) → ALWAYS ALLOWED
           ------------------------------------------------------------ */
        if (action == InventoryAction.DROP_ALL_CURSOR ||
                action == InventoryAction.DROP_ONE_CURSOR ||
                action == InventoryAction.DROP_ALL_SLOT ||
                action == InventoryAction.DROP_ONE_SLOT) {
            return;
        }

        /* ------------------------------------------------------------
           ONLY CHECK ACTIONS THAT ADD ITEMS TO PLAYER INVENTORY
           ------------------------------------------------------------ */
        boolean addsToInventory =
                action == InventoryAction.PLACE_ALL ||
                        action == InventoryAction.PLACE_ONE ||
                        action == InventoryAction.PLACE_SOME ||
                        action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

        if (!addsToInventory) return;

        ItemStack adding =
                action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                        ? current
                        : cursor;

        if (adding == null || adding.getType() == Material.AIR) return;

        Material material = adding.getType();

        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);

        // Banned everywhere
        if (limit == 0) {
            event.setCancelled(true);
            player.updateInventory();
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                            "messages.item-blocked-place-banned",
                            "&cYou cannot use &e{item}&c - it is banned!"
                    ).replace("{item}", format(material))
            ));
            return;
        }

        int currentCount = itemLimitManager.countItemInInventory(player, material);

        // At or above limit → block ADDING, but NOT removing
        if (currentCount >= limit) {
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

    /* ============================================================
       LOGIN ENFORCEMENT (ONLY PLACE WE REMOVE ITEMS)
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
