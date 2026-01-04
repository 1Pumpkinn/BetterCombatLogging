package net.saturn.limiter.listener;

import net.saturn.BetterCombatLogging;
import net.saturn.limiter.manager.ItemLimitManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
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
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem().getItemStack();
        Material material = stack.getType();

        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        if (limit == 0 || current + stack.getAmount() > limit) {
            event.setCancelled(true);
            sendBlockedMessage(player, material, limit);
        }
    }

    /* ============================================================
       INVENTORY CLICK
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getType() == InventoryType.CREATIVE) return;

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) return;

        Inventory playerInv = player.getInventory();
        InventoryAction action = event.getAction();

        ItemStack moving;

        boolean fromContainer = clicked != playerInv;

        switch (action) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
            case COLLECT_TO_CURSOR:
                moving = event.getCursor();
                fromContainer = false;
                break;

            case MOVE_TO_OTHER_INVENTORY:
                moving = event.getCurrentItem();
                break;

            default:
                return;
        }

        if (moving == null || moving.getType() == Material.AIR) return;

        Material material = moving.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        if (!isAddingToPlayer(action, clicked, playerInv)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        if (limit == 0 || current + moving.getAmount() > limit) {
            event.setCancelled(true);
            player.updateInventory();

            // Only drop if NOT from a container
            if (!fromContainer) {
                dropCursorSafe(player);
            }

            sendBlockedMessage(player, material, limit);
        }
    }

    /* ============================================================
       INVENTORY CLOSE (ESC)
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

        if (limit == 0 || current + cursor.getAmount() > limit) {
            dropCursorSafe(player);
            sendBlockedMessage(player, material, limit);
        }
    }

    /* ============================================================
       DROP KEY (Q)
       ============================================================ */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        Material material = cursor.getType();
        if (!itemLimitManager.isItemLimited(material)) return;

        int limit = itemLimitManager.getLimit(material);
        int current = itemLimitManager.countItemInInventory(player, material);

        if (limit == 0 || current + cursor.getAmount() > limit) {
            event.setCancelled(true);
            dropCursorSafe(player);
            sendBlockedMessage(player, material, limit);
        }
    }

    /* ============================================================
       LOGIN ENFORCEMENT
       ============================================================ */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
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
                                    "&cRemoved &e{count} &climited items!"
                            ).replace("{count}", String.valueOf(removed))
                    ));
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    /* ============================================================
       HELPERS
       ============================================================ */
    private boolean isAddingToPlayer(InventoryAction action, Inventory clicked, Inventory playerInv) {
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return clicked != playerInv;
        }
        return clicked == playerInv;
    }

    private void dropCursorSafe(Player player) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        ItemStack drop = cursor.clone();
        player.setItemOnCursor(null);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }.runTask(plugin);
    }

    private void sendBlockedMessage(Player player, Material material, int limit) {
        player.sendMessage(colorize(
                plugin.getConfig().getString(
                                limit == 0
                                        ? "messages.item-blocked-place-banned"
                                        : "messages.item-blocked-place-limit",
                                "&cYou cannot have &e{item}&c!"
                        )
                        .replace("{item}", format(material))
                        .replace("{limit}", String.valueOf(limit))
        ));
    }

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
