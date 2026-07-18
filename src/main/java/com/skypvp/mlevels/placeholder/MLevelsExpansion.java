package com.skypvp.mlevels.placeholder;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.model.Level;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * Registers the following placeholders (works in any tab/scoreboard plugin
 * that supports PlaceholderAPI):
 *
 *   %mlevels_level%     -> current level number (e.g. 7)
 *   %mlevels_peak%      -> highest level ever reached (drives actual gear)
 *   %mlevels_stage%     -> current level's name (e.g. "Diamond Sword Sharpness III")
 *   %mlevels_maxlevel%  -> max possible level (118 by default)
 *   %mlevels_progress%  -> progress percentage (e.g. 34%)
 *
 * Note: the level updates live with every kill/death.
 */
public class MLevelsExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public MLevelsExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "mlevels";
    }

    @Override
    public String getAuthor() {
        return "SkyPvP";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        int level = plugin.getPlayerDataManager().getLevel(player.getUniqueId());
        int peak = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        switch (params.toLowerCase()) {
            case "":
            case "level":
                return String.valueOf(level);
            case "peak":
                return String.valueOf(peak);
            case "stage":
                Level lvl = plugin.getLevelManager().getLevel(peak);
                return lvl != null ? lvl.getName() : "None";
            case "maxlevel":
                return String.valueOf(maxLevel);
            case "progress":
                if (maxLevel <= 0) return "0%";
                int percent = (int) (((double) peak / maxLevel) * 100);
                return percent + "%";
            default:
                return "";
        }
    }
}
