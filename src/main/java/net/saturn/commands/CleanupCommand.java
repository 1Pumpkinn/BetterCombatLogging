package net.saturn.commands;

import net.saturn.BetterCombatLogging;
import net.saturn.tasks.MapCleanupTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CleanupCommand implements CommandExecutor, TabCompleter {

    private final BetterCombatLogging plugin;
    private long lastCleanup = 0;
    private static final long COOLDOWN_MS = 5000; // 5 second cooldown

    public CleanupCommand(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            return handleCleanup(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "now":
            case "run":
                return handleCleanup(sender);
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

    private boolean handleCleanup(CommandSender sender) {
        // Check cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup < COOLDOWN_MS) {
            long remaining = (COOLDOWN_MS - (currentTime - lastCleanup)) / 1000;
            sender.sendMessage(colorize("&cPlease wait " + remaining + " seconds before running cleanup again!"));
            return true;
        }

        sender.sendMessage(colorize("&eStarting manual map cleanup..."));

        // Run cleanup task synchronously
        MapCleanupTask cleanupTask = new MapCleanupTask(plugin);
        cleanupTask.run();

        lastCleanup = currentTime;
        return true;
    }

    private boolean handleToggle(CommandSender sender) {
        boolean currentState = plugin.getConfig().getBoolean("cleanup.enabled", true);
        boolean newState = !currentState;

        plugin.getConfig().set("cleanup.enabled", newState);
        plugin.saveConfig();

        if (newState) {
            sender.sendMessage(colorize("&aAutomatic map cleanup has been &nenabled&r&a!"));
            sender.sendMessage(colorize("&7The cleanup task will restart on next server reload."));
        } else {
            sender.sendMessage(colorize("&cAutomatic map cleanup has been &ndisabled&r&c!"));
            sender.sendMessage(colorize("&7You can still run manual cleanups with /cleanup"));
        }

        return true;
    }

    private boolean handleInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            int current = plugin.getConfig().getInt("cleanup.interval-minutes", 30);
            sender.sendMessage(colorize("&eCurrent cleanup interval: &6" + current + " minutes"));
            sender.sendMessage(colorize("&7Usage: /cleanup interval <minutes>"));
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

        plugin.getConfig().set("cleanup.interval-minutes", minutes);
        plugin.saveConfig();

        sender.sendMessage(colorize("&aCleanup interval set to &e" + minutes + " minutes"));
        sender.sendMessage(colorize("&7This will take effect after the next server reload."));

        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        boolean enabled = plugin.getConfig().getBoolean("cleanup.enabled", true);
        int interval = plugin.getConfig().getInt("cleanup.interval-minutes", 30);
        boolean broadcast = plugin.getConfig().getBoolean("cleanup.broadcast-cleanup", true);

        sender.sendMessage(colorize("&6&m----------&r &e&lCleanup Status &6&m----------"));
        sender.sendMessage(colorize("&7Enabled: " + (enabled ? "&aYes" : "&cNo")));
        sender.sendMessage(colorize("&7Interval: &e" + interval + " minutes"));
        sender.sendMessage(colorize("&7Broadcast: " + (broadcast ? "&aYes" : "&cNo")));
        sender.sendMessage(colorize("&7Blocks Removed: &eCobwebs, Water, Lava"));
        sender.sendMessage(colorize("&6&m---------------------------------------"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&m----------&r &e&lMap Cleanup &6&m----------"));
        sender.sendMessage(colorize("&e/cleanup &7- Run cleanup immediately"));
        sender.sendMessage(colorize("&e/cleanup now &7- Run cleanup immediately"));
        sender.sendMessage(colorize("&e/cleanup toggle &7- Enable/disable auto cleanup"));
        sender.sendMessage(colorize("&e/cleanup interval <minutes> &7- Set cleanup interval"));
        sender.sendMessage(colorize("&e/cleanup status &7- View cleanup settings"));
        sender.sendMessage(colorize("&6&m---------------------------------------"));
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("now", "run", "toggle", "interval", "status"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("interval")) {
            completions.addAll(Arrays.asList("5", "10", "15", "30", "60"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}