package net.saturn.managers.regions;

import net.saturn.BetterCombatLogging;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RegionManager {

    private final BetterCombatLogging plugin;
    private final Set<String> blockedRegions;
    private File dataFile;
    private FileConfiguration data;

    public RegionManager(BetterCombatLogging plugin) {
        this.plugin = plugin;
        this.blockedRegions = new HashSet<>();
        this.dataFile = new File(plugin.getDataFolder(), "blocked-regions.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create blocked-regions.yml: " + e.getMessage());
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load blocked regions
        if (data.contains("blocked-regions")) {
            blockedRegions.addAll(data.getStringList("blocked-regions"));
            plugin.getLogger().info("Loaded " + blockedRegions.size() + " blocked regions");
        } else {
            // Load from config.yml for backwards compatibility
            List<String> configRegions = plugin.getConfig().getStringList("blocked-regions");
            if (!configRegions.isEmpty()) {
                blockedRegions.addAll(configRegions);
                plugin.getLogger().info("Migrated " + blockedRegions.size() + " blocked regions from config.yml");
                save(); // Save to new file
            }
        }
    }

    public void save() {
        try {
            data.set("blocked-regions", new ArrayList<>(blockedRegions));
            data.save(dataFile);
            plugin.getLogger().info("Saved blocked regions");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blocked regions: " + e.getMessage());
        }
    }

    public boolean addRegion(String regionName) {
        boolean added = blockedRegions.add(regionName);
        if (added) {
            save();
        }
        return added;
    }

    public boolean removeRegion(String regionName) {
        boolean removed = blockedRegions.remove(regionName);
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean isRegionBlocked(String regionName) {
        return blockedRegions.contains(regionName);
    }

    public List<String> getBlockedRegions() {
        return new ArrayList<>(blockedRegions);
    }

    public int getBlockedRegionCount() {
        return blockedRegions.size();
    }

    public void clearRegions() {
        blockedRegions.clear();
        save();
    }
}