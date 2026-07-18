package com.skypvp.mlevels.commands;

import com.skypvp.mlevels.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LevelCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public LevelCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;

        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().color("&cPlayer is not online"));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().color("&cUse /level <player> from console"));
                return true;
            }
            target = (Player) sender;
        }

        int level = plugin.getPlayerDataManager().getLevel(target.getUniqueId());
        int peak = plugin.getPlayerDataManager().getPeakLevel(target.getUniqueId());
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        sender.sendMessage(plugin.getConfigManager().msg("level-check")
                .replace("{player}", target.getName())
                .replace("{level}", String.valueOf(level))
                .replace("{peak}", String.valueOf(peak))
                .replace("{max_level}", String.valueOf(maxLevel)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
            }
            return names;
        }
        return new ArrayList<>();
    }
}
