package com.skypvp.mlevels.managers;

import com.skypvp.mlevels.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    // death-penalty tiers, sorted ascending by "up-to-level"
    private final List<int[]> penaltyTiers = new ArrayList<>(); // [upToLevel, penaltyAmount]
    private int defaultPenalty = 2;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadPenaltyTiers();
    }

    public void reload() {
        this.config = plugin.getConfig();
        penaltyTiers.clear();
        loadPenaltyTiers();
    }

    private void loadPenaltyTiers() {
        defaultPenalty = config.getInt("death-penalty.default-loss", 2);

        ConfigurationSection section = config.getConfigurationSection("death-penalty.tiers");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int upToLevel = config.getInt("death-penalty.tiers." + key + ".up-to-level", Integer.MAX_VALUE);
            int loss = config.getInt("death-penalty.tiers." + key + ".levels-lost", defaultPenalty);
            penaltyTiers.add(new int[]{upToLevel, loss});
        }
        penaltyTiers.sort((a, b) -> Integer.compare(a[0], b[0]));
    }

    /**
     * Returns how many levels a player at the given peak-level should lose on death.
     * Tiers are checked in ascending "up-to-level" order; the first tier whose
     * up-to-level is >= the player's peak level applies. If the player's peak
     * level exceeds every tier, the last (highest) tier's loss applies.
     */
    public int getDeathPenaltyForLevel(int peakLevel) {
        if (penaltyTiers.isEmpty()) return defaultPenalty;
        for (int[] tier : penaltyTiers) {
            if (peakLevel <= tier[0]) return tier[1];
        }
        return penaltyTiers.get(penaltyTiers.size() - 1)[1];
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public boolean isBroadcastLevelUp() {
        return config.getBoolean("general.broadcast-level-up", true);
    }

    public String msg(String path) {
        String prefix = color(config.getString("messages.prefix", ""));
        String raw = config.getString("messages." + path, "");
        return prefix + color(raw);
    }

    public String rawMsg(String path) {
        return color(config.getString("messages." + path, ""));
    }

    public String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
