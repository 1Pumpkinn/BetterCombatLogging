package net.saturn.commands.cleanup;

import net.saturn.BetterCombatLogging;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemClearCommand implements CommandExecutor, TabCompleter {

    private final BetterCombatLogging plugin;
    private long lastManualClear = 0;
    private static final long COOLDOWN_MS = 5000; // 5 second cooldown

    public ItemClearCommand(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            return handleClear(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "now":
            case "clear":
                return handleClear(sender);
            case "toggle":
                return handleToggle(sender);
            case "interval":
                return handleInterval(sender, args);
            case "status":
                return handleStatus(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleClear(CommandSender sender) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastManualClear < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (currentTime - lastManualClear)) / 1000;
            sender.sendMessage(colorize("&cPlease wait " + remaining + " seconds before clearing items again!"));
            return true;
        }

        sender.sendMessage(colorize("&eClearing all items..."));

        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (!plugin.getConfig().getBoolean("item-clear.worlds." + world.getName() + ".enabled", true)) {
                continue;
            }

            for (Item item : world.getEntitiesByClass(Item.class)) {
                item.remove();
                count++;
            }
        }

        sender.sendMessage(colorize("&aCleared &e" + count + " &aitems from the ground!"));
        lastManualClear = currentTime;
        return true;
    }

    private boolean handleToggle(CommandSender sender) {
        boolean currentState = plugin.getConfig().getBoolean("item-clear.enabled", true);
        boolean newState = !currentState;

        plugin.getConfig().set("item-clear.enabled", newState);
        plugin.saveConfig();

        if (newState) {
            sender.sendMessage(colorize("&aAutomatic item clearing has been &nenabled&r&a!"));
            sender.sendMessage(colorize("&7The task will restart on next server reload."));
        } else {
            sender.sendMessage(colorize("&cAutomatic item clearing has been &ndisabled&r&c!"));
            sender.sendMessage(colorize("&7You can still run manual clears with /itemclear"));
        }

        return true;
    }

    private boolean handleInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            int current = plugin.getConfig().getInt("item-clear.interval-minutes", 20);
            sender.sendMessage(colorize("&eCurrent item clear interval: &6" + current + " minutes"));
            sender.sendMessage(colorize("&7Usage: /itemclear interval <minutes>"));
            return true;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid number! Must be a valid integer."));
            return true;
        }

        if (minutes < 1) {
            sender.sendMessage(colorize("&cInterval must be at least 1 minute!"));
            return true;
        }

        if (minutes > 1440) {
            sender.sendMessage(colorize("&cInterval cannot exceed 1440 minutes (24 hours)!"));
            return true;
        }

        plugin.getConfig().set("item-clear.interval-minutes", minutes);
        plugin.saveConfig();

        sender.sendMessage(colorize("&aItem clear interval set to &e" + minutes + " minutes"));
        sender.sendMessage(colorize("&7This will take effect after the next server reload."));

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        boolean enabled = plugin.getConfig().getBoolean("item-clear.enabled", true);
        int interval = plugin.getConfig().getInt("item-clear.interval-minutes", 20);
        boolean announceWarning = plugin.getConfig().getBoolean("item-clear.announce-warning", true);
        boolean announceClear = plugin.getConfig().getBoolean("item-clear.announce-clear", true);
        int warningSeconds = plugin.getConfig().getInt("item-clear.warning-seconds", 30);

        sender.sendMessage(colorize("&6&m----------&r &e&lItem Clear Status &6&m----------"));
        sender.sendMessage(colorize("&7Enabled: " + (enabled ? "&aYes" : "&cNo")));
        sender.sendMessage(colorize("&7Interval: &e" + interval + " minutes"));
        sender.sendMessage(colorize("&7Warning: " + (announceWarning ? "&aEnabled &7(" + warningSeconds + "s)" : "&cDisabled")));
        sender.sendMessage(colorize("&7Announce Clear: " + (announceClear ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(colorize("&6&m---------------------------------------"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&m----------&r &e&lItem Clear &6&m----------"));
        sender.sendMessage(colorize("&e/itemclear &7- Clear items immediately"));
        sender.sendMessage(colorize("&e/itemclear now &7- Clear items immediately"));
        sender.sendMessage(colorize("&e/itemclear toggle &7- Enable/disable auto clearing"));
        sender.sendMessage(colorize("&e/itemclear interval <minutes> &7- Set clear interval"));
        sender.sendMessage(colorize("&e/itemclear status &7- View current settings"));
        sender.sendMessage(colorize("&6&m---------------------------------------"));
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("now", "clear", "toggle", "interval", "status"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("interval")) {
            completions.addAll(Arrays.asList("5", "10", "15", "20", "30", "60"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}