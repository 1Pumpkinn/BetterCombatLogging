package net.saturn.commands.combat;

import net.saturn.BetterCombatLogging;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CombatDurationCommand implements CommandExecutor, TabCompleter {

    private final BetterCombatLogging plugin;

    public CombatDurationCommand(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            int current = plugin.getConfig().getInt("combat-duration", 15);
            sender.sendMessage(colorize("&eCurrent combat duration: &6" + current + " seconds"));
            sender.sendMessage(colorize("&7Use: /setcombatduration <seconds>"));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid number! Must be a valid integer."));
            return true;
        }

        if (seconds < 1) {
            sender.sendMessage(colorize("&cCombat duration must be at least 1 second!"));
            return true;
        }

        if (seconds > 300) {
            sender.sendMessage(colorize("&cCombat duration cannot exceed 300 seconds (5 minutes)!"));
            return true;
        }

        plugin.getConfig().set("combat-duration", seconds);
        plugin.saveConfig();

        sender.sendMessage(colorize("&aCombat duration set to &e" + seconds + " seconds"));
        sender.sendMessage(colorize("&7Note: This will apply to new combat tags. Existing combat tags will continue with their original duration."));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("5", "10", "15", "20", "30", "60").stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}