package net.saturn.commands.combat;

import net.saturn.managers.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CombatTestCommand implements CommandExecutor, TabCompleter {

    private final CombatManager combatManager;

    public CombatTestCommand(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("BetterCombatLogging.admin")) {
            sender.sendMessage(colorize("&cYou don't have permission to use this command!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(colorize("&cUsage: /activatecombat <player>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(colorize("&cPlayer &e" + targetName + " &cis not online!"));
            return true;
        }

        combatManager.tagPlayer(target);
        sender.sendMessage(colorize("&aActivated combat mode for &e" + target.getName()));

        if (!sender.equals(target)) {
            target.sendMessage(colorize("&eYour combat mode has been activated by an administrator."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String colorize(String message) {
        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', message);
    }
}