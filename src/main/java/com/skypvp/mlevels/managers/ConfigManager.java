package com.skypvp.mlevels.managers;

import com.skypvp.mlevels.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    private int fullResetBelowLevel;
    private int baseTierStart;
    private int baseLoss;
    private int stepTierStart;
    private int stepLoss;
    private int stepSize;
    private int stepIncrement;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadPenaltySettings();
    }

    public void reload() {
        this.config = plugin.getConfig();
        loadPenaltySettings();
    }

    private void loadPenaltySettings() {
        // Default rules:
        //   peak level < 25           -> full reset (level and peak both go to 0)
        //   25 <= peak level < 35     -> lose 4 levels
        //   35 <= peak level < 50     -> lose 5 levels
        //   every extra "step-size" levels above "step-tier-start" adds
        //   "step-increment" more levels lost (50-64 -> 6, 65-79 -> 7, ...)
        fullResetBelowLevel = config.getInt("death-penalty.full-reset-below-level", 25);
        baseTierStart = config.getInt("death-penalty.base-tier-start", 25);
        baseLoss = config.getInt("death-penalty.base-loss", 4);
        stepTierStart = config.getInt("death-penalty.step-tier-start", 35);
        stepLoss = config.getInt("death-penalty.step-loss", 5);
        stepSize = config.getInt("death-penalty.step-size", 15);
        stepIncrement = config.getInt("death-penalty.step-increment", 1);
    }

    /** True when a death at this peak level should fully reset the player back to level 0. */
    public boolean isFullResetLevel(int peakLevel) {
        return peakLevel < fullResetBelowLevel;
    }

    /**
     * How many levels a player at the given peak level loses on death, for
     * peak levels at/above {@code full-reset-below-level} (check
     * {@link #isFullResetLevel(int)} first - this method assumes it's false).
     */
    public int getDeathPenaltyForLevel(int peakLevel) {
        if (peakLevel < stepTierStart) {
            return baseLoss; // e.g. 25-34 -> 4
        }
        int stepsPast = (peakLevel - stepTierStart) / stepSize; // 0 for 35-49, 1 for 50-64, ...
        return stepLoss + (stepsPast * stepIncrement); // e.g. 35-49 -> 5, 50-64 -> 6, ...
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
