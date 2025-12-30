package net.saturn.commands;

import net.saturn.BetterCombatLogging;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ToggleVisualizerCommand implements CommandExecutor {

    private final BetterCombatLogging plugin;

    public ToggleVisualizerCommand(BetterCombatLogging plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (!plugin.isWorldGuardEnabled()) {
            sender.sendMessage(colorize("&cWorldGuard is not enabled! Region visualizer is unavailable."));
            return true;
        }

        boolean currentState = plugin.getConfig().getBoolean("region-visualizer.enabled", true);
        boolean newState = !currentState;

        plugin.getConfig().set("region-visualizer.enabled", newState);
        plugin.saveConfig();

        if (newState) {
            if (plugin.getRegionVisualizer() != null) {
                plugin.getRegionVisualizer().start();
            }
            sender.sendMessage(colorize("&aRegion visualizer has been &nenabled&r&a!"));
            sender.sendMessage(colorize("&7Players in combat will see red glass panes around blocked regions."));
        } else {
            if (plugin.getRegionVisualizer() != null) {
                plugin.getRegionVisualizer().stop();
            }
            sender.sendMessage(colorize("&cRegion visualizer has been &ndisabled&r&c!"));
        }

        return true;
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}