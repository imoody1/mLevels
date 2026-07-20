package com.skypvp.mlevels.managers;

import com.skypvp.mlevels.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Main plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    private final Map<UUID, Integer> levels = new HashMap<>();
    private final Map<UUID, Integer> peakLevels = new HashMap<>();

    public PlayerDataManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playerdata.yml: " + e.getMessage());
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!dataConfig.isConfigurationSection("players")) return;
        for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                levels.put(uuid, dataConfig.getInt("players." + key + ".level", 0));
                peakLevels.put(uuid, dataConfig.getInt("players." + key + ".peak-level", 0));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public int getLevel(UUID uuid) {
        return levels.getOrDefault(uuid, 0);
    }

    public int getPeakLevel(UUID uuid) {
        return peakLevels.getOrDefault(uuid, 0);
    }

    public void setLevel(UUID uuid, int level) {
        levels.put(uuid, level);
        dataConfig.set("players." + uuid + ".level", level);
    }

    public void setPeakLevel(UUID uuid, int peak) {
        peakLevels.put(uuid, peak);
        dataConfig.set("players." + uuid + ".peak-level", peak);
    }

    public void reset(UUID uuid) {
        setLevel(uuid, 0);
        setPeakLevel(uuid, 0);
    }

    public void saveAll() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save playerdata.yml: " + e.getMessage());
        }
    }
}