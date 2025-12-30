package net.saturn.commands;

import net.saturn.BetterCombatLogging;
import net.saturn.managers.regions.RegionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BlockedRegionCommand implements CommandExecutor, TabCompleter {

    private final BetterCombatLogging plugin;
    private final RegionManager regionManager;

    public BlockedRegionCommand(BetterCombatLogging plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        // Check if WorldGuard is enabled
        if (!plugin.isWorldGuardEnabled()) {
            sender.sendMessage(colorize("&cWorldGuard is not enabled! This feature requires WorldGuard."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "clear":
                return handleClear(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /blockedregion add <region>"));
            sender.sendMessage(colorize("&7Example: /blockedregion add spawn"));
            return true;
        }

        String regionName = args[1];

        if (regionManager.isRegionBlocked(regionName)) {
            sender.sendMessage(colorize("&cRegion &e" + regionName + " &cis already blocked!"));
            return true;
        }

        regionManager.addRegion(regionName);
        sender.sendMessage(colorize("&aAdded &e" + regionName + " &ato blocked regions!"));
        sender.sendMessage(colorize("&7Players in combat will not be able to enter this region."));

        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /blockedregion remove <region>"));
            sender.sendMessage(colorize("&7Example: /blockedregion remove spawn"));
            return true;
        }

        String regionName = args[1];

        if (!regionManager.isRegionBlocked(regionName)) {
            sender.sendMessage(colorize("&cRegion &e" + regionName + " &cis not currently blocked!"));
            return true;
        }

        regionManager.removeRegion(regionName);
        sender.sendMessage(colorize("&aRemoved &e" + regionName + " &afrom blocked regions!"));

        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> blockedRegions = regionManager.getBlockedRegions();

        if (blockedRegions.isEmpty()) {
            sender.sendMessage(colorize("&eNo regions are currently blocked."));
            sender.sendMessage(colorize("&7Use &e/blockedregion add <region> &7to block a region."));
            return true;
        }

        sender.sendMessage(colorize("&6&m----------&r &e&lBlocked Regions &6&m----------"));
        sender.sendMessage(colorize("&7Total: &e" + blockedRegions.size()));
        sender.sendMessage("");

        for (String region : blockedRegions) {
            sender.sendMessage(colorize("&8• &e" + region));
        }

        sender.sendMessage(colorize("&6&m---------------------------------------"));

        return true;
    }

    private boolean handleClear(CommandSender sender) {
        int count = regionManager.getBlockedRegionCount();

        if (count == 0) {
            sender.sendMessage(colorize("&cNo regions are currently blocked!"));
            return true;
        }

        regionManager.clearRegions();
        sender.sendMessage(colorize("&aCleared all &e" + count + " &ablocked regions!"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&m----------&r &e&lBlocked Regions &6&m----------"));
        sender.sendMessage(colorize("&e/blockedregion add <region> &7- Block a region"));
        sender.sendMessage(colorize("&e/blockedregion remove <region> &7- Unblock a region"));
        sender.sendMessage(colorize("&e/blockedregion list &7- List all blocked regions"));
        sender.sendMessage(colorize("&e/blockedregion clear &7- Clear all blocked regions"));
        sender.sendMessage(colorize("&6&m---------------------------------------"));
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("add", "remove", "list", "clear"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // Suggest currently blocked regions for removal
            completions.addAll(regionManager.getBlockedRegions());
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}