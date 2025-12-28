package net.saturn.managers;

import net.saturn.BetterCombatLogging;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProtectionManager {

    private final BetterCombatLogging plugin;
    private final Map<String, Integer> worldLimits;
    private File dataFile;
    private FileConfiguration data;

    public ProtectionManager(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.worldLimits = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "protection-limits.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            plugin.saveResource("protection-limits.yml", false);
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load world limits
        if (data.contains("world-limits")) {
            for (String world : data.getConfigurationSection("world-limits").getKeys(false)) {
                worldLimits.put(world, data.getInt("world-limits." + world));
            }
        }

        plugin.getLogger().info("Loaded " + worldLimits.size() + " world protection limits");
    }

    public void save() {
        try {
            // Clear existing data
            data.set("world-limits", null);

            // Save all world limits
            for (Map.Entry<String, Integer> entry : worldLimits.entrySet()) {
                data.set("world-limits." + entry.getKey(), entry.getValue());
            }

            data.save(dataFile);
            plugin.getLogger().info("Saved protection limits");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save protection limits: " + e.getMessage());
        }
    }

    public void setLimit(String worldName, int level) {
        if (level <= 0) {
            worldLimits.remove(worldName);
        } else {
            worldLimits.put(worldName, level);
        }
        save();
    }

    public Integer getLimit(String worldName) {
        return worldLimits.get(worldName);
    }

    public boolean hasLimit(String worldName) {
        return worldLimits.containsKey(worldName);
    }

    public Map<String, Integer> getAllLimits() {
        return new HashMap<>(worldLimits);
    }

    /**
     * Enforces protection enchantment limits on an item
     * @param item The item to check and modify
     * @param worldName The world the player is in
     * @return true if the item was modified, false otherwise
     */
    public boolean enforceLimit(ItemStack item, String worldName) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Integer limit = getLimit(worldName);
        if (limit == null) {
            return false; // No limit set for this world
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchant(Enchantment.PROTECTION)) {
            return false; // No protection enchantment
        }

        int currentLevel = meta.getEnchantLevel(Enchantment.PROTECTION);
        if (currentLevel <= limit) {
            return false; // Already within limit
        }

        // Remove and re-add with limited level
        meta.removeEnchant(Enchantment.PROTECTION);
        meta.addEnchant(Enchantment.PROTECTION, limit, true);
        item.setItemMeta(meta);

        return true; // Item was modified
    }

    /**
     * Checks if an item exceeds the protection limit
     * @param item The item to check
     * @param worldName The world the player is in
     * @return true if item exceeds limit, false otherwise
     */
    public boolean exceedsLimit(ItemStack item, String worldName) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Integer limit = getLimit(worldName);
        if (limit == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchant(Enchantment.PROTECTION)) {
            return false;
        }

        return meta.getEnchantLevel(Enchantment.PROTECTION) > limit;
    }
}