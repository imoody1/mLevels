package com.skypvp.mlevels.commands;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.model.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MLevelsCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public MLevelsCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels reload &7- reload the config"));
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels set <player> <level> &7- set a player's level"));
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels add <player> <amount> &7- add levels to a player"));
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels remove <player> <amount> &7- remove levels from a player"));
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels check [player] &7- show a player's current level"));
            sender.sendMessage(plugin.getConfigManager().color("&e/mlevels reset <player> &7- reset a player's level and progress"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": {
                if (!sender.hasPermission("mlevels.admin")) {
                    sender.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(plugin.getConfigManager().msg("reloaded"));
                return true;
            }
            case "set": {
                if (!sender.hasPermission("mlevels.admin")) {
                    sender.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().color("&cUsage: /mlevels set <player> <level>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cPlayer is not online"));
                    return true;
                }
                Integer levelNum = parseInt(args[2]);
                if (levelNum == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cInvalid level number"));
                    return true;
                }
                int clamped = Math.max(0, Math.min(levelNum, plugin.getLevelManager().getMaxLevel()));
                setPlayerLevel(target, clamped);
                sender.sendMessage(plugin.getConfigManager().msg("level-set")
                        .replace("{player}", target.getName())
                        .replace("{level}", String.valueOf(clamped)));
                return true;
            }
            case "add": {
                if (!sender.hasPermission("mlevels.admin")) {
                    sender.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().color("&cUsage: /mlevels add <player> <amount>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cPlayer is not online"));
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cInvalid amount"));
                    return true;
                }
                int current = plugin.getPlayerDataManager().getLevel(target.getUniqueId());
                int newLevel = Math.min(current + amount, plugin.getLevelManager().getMaxLevel());
                setPlayerLevel(target, newLevel);
                sender.sendMessage(plugin.getConfigManager().msg("level-added")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{player}", target.getName())
                        .replace("{level}", String.valueOf(newLevel)));
                return true;
            }
            case "remove": {
                if (!sender.hasPermission("mlevels.admin")) {
                    sender.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getConfigManager().color("&cUsage: /mlevels remove <player> <amount>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cPlayer is not online"));
                    return true;
                }
                Integer amount = parseInt(args[2]);
                if (amount == null) {
                    sender.sendMessage(plugin.getConfigManager().color("&cInvalid amount"));
                    return true;
                }
                int current = plugin.getPlayerDataManager().getLevel(target.getUniqueId());
                int newLevel = Math.max(0, current - amount);
                // Note: this only lowers the visible level counter, same as a death penalty -
                // it does not strip the player's already-earned gear (peak level stays put).
                plugin.getPlayerDataManager().setLevel(target.getUniqueId(), newLevel);
                sender.sendMessage(plugin.getConfigManager().msg("level-removed")
                        .replace("{amount}", String.valueOf(amount))
                        .replace("{player}", target.getName())
                        .replace("{level}", String.valueOf(newLevel)));
                return true;
            }
            case "check": {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getConfigManager().color("&cPlayer is not online"));
                        return true;
                    }
                } else if (sender instanceof Player) {
                    target = (Player) sender;
                } else {
                    sender.sendMessage(plugin.getConfigManager().color("&cSpecify a player name"));
                    return true;
                }
                int lvl = plugin.getPlayerDataManager().getLevel(target.getUniqueId());
                int peak = plugin.getPlayerDataManager().getPeakLevel(target.getUniqueId());
                sender.sendMessage(plugin.getConfigManager().msg("level-check")
                        .replace("{player}", target.getName())
                        .replace("{level}", String.valueOf(lvl))
                        .replace("{peak}", String.valueOf(peak))
                        .replace("{max_level}", String.valueOf(plugin.getLevelManager().getMaxLevel())));
                return true;
            }
            case "reset": {
                if (!sender.hasPermission("mlevels.admin")) {
                    sender.sendMessage(plugin.getConfigManager().msg("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().color("&cUsage: /mlevels reset <player>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                UUID uuid = target.getUniqueId();
                plugin.getPlayerDataManager().reset(uuid);
                if (target.isOnline()) {
                    Player online = (Player) target;
                    // strip existing sword/axe/armor since progress is gone, then give the starter/rank kit
                    online.getInventory().setHelmet(null);
                    online.getInventory().setChestplate(null);
                    online.getInventory().setLeggings(null);
                    online.getInventory().setBoots(null);
                    plugin.getGearListener().grantStarterKit(online);
                }
                sender.sendMessage(plugin.getConfigManager().msg("level-reset")
                        .replace("{player}", args[1]));
                return true;
            }
            default:
                sender.sendMessage(plugin.getConfigManager().color("&cUnknown command. Use /mlevels"));
                return true;
        }
    }

    private void setPlayerLevel(Player target, int newLevel) {
        UUID id = target.getUniqueId();
        plugin.getPlayerDataManager().setLevel(id, newLevel);
        int peak = plugin.getPlayerDataManager().getPeakLevel(id);
        if (newLevel > peak) {
            plugin.getPlayerDataManager().setPeakLevel(id, newLevel);
        }
        Level level = plugin.getLevelManager().getLevel(Math.max(newLevel, peak));
        if (level != null) {
            plugin.getLevelManager().applyGear(target, level);
        }
    }

    private Integer parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("reload", "set", "add", "remove", "check", "reset"), args[0]);
        }
        if (args.length == 2 && Arrays.asList("set", "add", "remove", "check", "reset").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filter(names, args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }
}
