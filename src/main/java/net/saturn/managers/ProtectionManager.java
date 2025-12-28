package net.saturn.managers;

import net.saturn.BetterCombatLogging;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;

public class ProtectionManager {

    private final BetterCombatLogging plugin;
    private Integer globalLimit; // null means no limit
    private File dataFile;
    private FileConfiguration data;

    public ProtectionManager(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.globalLimit = null;
        this.dataFile = new File(plugin.getDataFolder(), "protection-limits.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create protection-limits.yml: " + e.getMessage());
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load global limit
        if (data.contains("global-limit")) {
            globalLimit = data.getInt("global-limit");
            plugin.getLogger().info("Loaded global protection limit: " + globalLimit);
        } else {
            plugin.getLogger().info("No global protection limit set");
        }
    }

    public void save() {
        try {
            // Clear existing data
            data.set("global-limit", null);

            // Save global limit if set
            if (globalLimit != null && globalLimit > 0) {
                data.set("global-limit", globalLimit);
            }

            data.save(dataFile);
            plugin.getLogger().info("Saved protection limits");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save protection limits: " + e.getMessage());
        }
    }

    public void setLimit(int level) {
        if (level <= 0) {
            globalLimit = null;
        } else {
            globalLimit = level;
        }
        save();
    }

    public Integer getLimit() {
        return globalLimit;
    }

    public boolean hasLimit() {
        return globalLimit != null && globalLimit > 0;
    }

    /**
     * Enforces protection enchantment limits on an item
     * @param item The item to check and modify
     * @return true if the item was modified, false otherwise
     */
    public boolean enforceLimit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (!hasLimit()) {
            return false; // No limit set
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchant(Enchantment.PROTECTION)) {
            return false; // No protection enchantment
        }

        int currentLevel = meta.getEnchantLevel(Enchantment.PROTECTION);
        if (currentLevel <= globalLimit) {
            return false; // Already within limit
        }

        // Remove and re-add with limited level
        meta.removeEnchant(Enchantment.PROTECTION);
        meta.addEnchant(Enchantment.PROTECTION, globalLimit, true);
        item.setItemMeta(meta);

        return true; // Item was modified
    }

    /**
     * Checks if an item exceeds the protection limit
     * @param item The item to check
     * @return true if item exceeds limit, false otherwise
     */
    public boolean exceedsLimit(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        if (!hasLimit()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchant(Enchantment.PROTECTION)) {
            return false;
        }

        return meta.getEnchantLevel(Enchantment.PROTECTION) > globalLimit;
    }
}