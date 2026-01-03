package net.saturn.limiter.listener;

import net.saturn.BetterCombatLogging;
import net.saturn.limiter.manager.ItemLimitManager;
import org.bukkit.Material;
import org.bukkit.entity.Item;
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

        if (limit == 0 || current + stack.getAmount() > limit) {
            event.setCancelled(true);
            player.sendMessage(colorize(
                    plugin.getConfig().getString(
                                    "messages.item-blocked-pickup",
                                    "&cYou cannot pick up &e{item}&c (limit: {limit})"
                            )
                            .replace("{item}", format(material))
                            .replace("{limit}", String.valueOf(limit))
            ));
        }
    }

    /* ============================================================
       INVENTORY CLICK
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() == InventoryType.CREATIVE) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        Inventory playerInv = player.getInventory();
        InventoryAction action = event.getAction();

        // Always allow removing items from containers
        if (clicked != playerInv) return;

        // Always allow dropping
        if (action.name().startsWith("DROP")) return;

        boolean adds =
                action == InventoryAction.PLACE_ALL ||
                        action == InventoryAction.PLACE_ONE ||
                        action == InventoryAction.PLACE_SOME ||
                        action == InventoryAction.MOVE_TO_OTHER_INVENTORY;

        if (!adds) return;

        ItemStack adding =
                action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                        ? event.getCurrentItem()
                        : event.getCursor();

        if (adding == null || adding.getType() == Material.AIR) return;

        Material material = adding.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        if (limit == 0 || current >= limit) {
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
       ESC / INVENTORY CLOSE FIX
       ============================================================ */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        Material material = cursor.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        if (limit == 0 || current + cursor.getAmount() > limit) {
            player.setItemOnCursor(null);
            player.getWorld().dropItemNaturally(player.getLocation(), cursor);
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
