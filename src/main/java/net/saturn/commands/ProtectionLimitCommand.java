package net.saturn.commands;

import net.saturn.BetterCombatLogging;
import net.saturn.managers.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProtectionLimitCommand implements CommandExecutor, TabCompleter {

    private final BetterCombatLogging plugin;
    private final ProtectionManager protectionManager;

    public ProtectionLimitCommand(BetterCombatLogging plugin, ProtectionManager protectionManager) {
        this.plugin = plugin;
        this.protectionManager = protectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set":
                return handleSet(sender, args);
            case "remove":
                return handleRemove(sender);
            case "check":
                return handleCheck(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /protectionlimit set <level>"));
            sender.sendMessage(colorize("&7Example: /protectionlimit set 3"));
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid level! Must be a number."));
            return true;
        }

        if (level < 1 || level > 4) {
            sender.sendMessage(colorize("&cProtection level must be between 1 and 4!"));
            return true;
        }

        protectionManager.setLimit(level);
        sender.sendMessage(colorize("&aGlobal protection limit set to &e" + level));

        // Notify all online players
        String message = plugin.getConfig().getString(
                "messages.protection-limit-set",
                "&eProtection enchantment has been limited to level {level} globally!"
        ).replace("{level}", String.valueOf(level));

        Bukkit.broadcastMessage(colorize(message));

        return true;
    }

    private boolean handleRemove(CommandSender sender) {
        if (!protectionManager.hasLimit()) {
            sender.sendMessage(colorize("&cNo global protection limit is currently set!"));
            return true;
        }

        protectionManager.setLimit(0); // 0 removes the limit
        sender.sendMessage(colorize("&aGlobal protection limit removed"));

        String message = plugin.getConfig().getString(
                "messages.protection-limit-removed",
                "&eProtection enchantment limit has been removed globally!"
        );
        Bukkit.broadcastMessage(colorize(message));

        return true;
    }

    private boolean handleCheck(CommandSender sender) {
        Integer limit = protectionManager.getLimit();

        if (limit == null) {
            sender.sendMessage(colorize("&eNo global protection limit is currently set."));
        } else {
            sender.sendMessage(colorize("&aGlobal protection limit: &6Protection " + limit));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&m----------&r &e&lProtection Limit &6&m----------"));
        sender.sendMessage(colorize("&e/protectionlimit set <level> &7- Set global limit (1-4)"));
        sender.sendMessage(colorize("&e/protectionlimit remove &7- Remove global limit"));
        sender.sendMessage(colorize("&e/protectionlimit check &7- Check current limit"));
        sender.sendMessage(colorize("&6&m---------------------------------------"));
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "remove", "check"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(Arrays.asList("1", "2", "3", "4"));
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}