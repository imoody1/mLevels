package com.skypvp.mlevels.managers;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.model.RankKit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified rank-kit system: each rank is just a permission + a console
 * command to run (e.g. hooking into another kits plugin like ExtremeKits/
 * KitsX/Kits). No item-giving logic here on purpose - keeping this thin is
 * what makes it reliable.
 */
public class KitManager {

    private final Main plugin;
    private final Map<String, RankKit> rankKits = new LinkedHashMap<>();
    private List<String> priority = new ArrayList<>();
    private String starterCommand;

    public KitManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        rankKits.clear();
        priority.clear();
        load();
    }

    private void load() {
        FileConfiguration config = plugin.getConfig();

        starterCommand = config.getString("starter-kit.command", "");
        priority = config.getStringList("rank-priority");

        ConfigurationSection section = config.getConfigurationSection("rank-kits");
        if (section == null) {
            plugin.getLogger().warning("No 'rank-kits' section found in config.yml!");
            return;
        }

        for (String rankId : section.getKeys(false)) {
            String permission = config.getString("rank-kits." + rankId + ".permission", "");
            String command = config.getString("rank-kits." + rankId + ".command", "");
            rankKits.put(rankId, new RankKit(rankId, permission, command));
        }

        plugin.getLogger().info("Loaded " + rankKits.size() + " rank kits from config.yml");
    }

    /**
     * Finds the highest-priority rank the player has permission for, and runs
     * its kit command. Falls back to the starter kit command if none match.
     */
    public void giveKitFor(Player player) {
        for (String rankId : priority) {
            RankKit kit = rankKits.get(rankId);
            if (kit == null || kit.getPermission() == null || kit.getPermission().isEmpty()) continue;
            if (player.hasPermission(kit.getPermission())) {
                runCommand(kit.getCommand(), player);
                return;
            }
        }
        runCommand(starterCommand, player);
    }

    private void runCommand(String command, Player player) {
        if (command == null || command.isEmpty()) return;
        String parsed = command.replace("%player%", player.getName()).replace("{player}", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
    }
}
