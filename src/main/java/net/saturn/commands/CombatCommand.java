package net.saturn.commands;

import net.saturn.managers.CombatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CombatCommand implements CommandExecutor {

    private final CombatManager combatManager;

    public CombatCommand(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cThis command can only be used by players!"));
            return true;
        }

        Player player = (Player) sender;

        if (combatManager.isInCombat(player)) {
            int remaining = combatManager.getRemainingTime(player);
            player.sendMessage(colorize("&cYou are in combat! &7Time remaining: &e" + remaining + "s"));
        } else {
            player.sendMessage(colorize("&aYou are not in combat."));
        }

        return true;
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}