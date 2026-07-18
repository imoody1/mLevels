package com.skypvp.mlevels;

import com.skypvp.mlevels.commands.LevelCommand;
import com.skypvp.mlevels.commands.MLevelsCommand;
import com.skypvp.mlevels.listeners.GearListener;
import com.skypvp.mlevels.managers.ConfigManager;
import com.skypvp.mlevels.managers.KitManager;
import com.skypvp.mlevels.managers.LevelManager;
import com.skypvp.mlevels.managers.PlayerDataManager;
import com.skypvp.mlevels.placeholder.MLevelsExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private LevelManager levelManager;
    private KitManager kitManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("progression.yml", false);

        this.configManager = new ConfigManager(this);
        this.levelManager = new LevelManager(this);
        this.kitManager = new KitManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        getServer().getPluginManager().registerEvents(new GearListener(this), this);

        MLevelsCommand adminCmd = new MLevelsCommand(this);
        getCommand("mlevels").setExecutor(adminCmd);
        getCommand("mlevels").setTabCompleter(adminCmd);

        LevelCommand levelCmd = new LevelCommand(this);
        getCommand("level").setExecutor(levelCmd);
        getCommand("level").setTabCompleter(levelCmd);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MLevelsExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI - %mlevels_level% is now available.");
        }

        getLogger().info("mLevels enabled successfully - " + levelManager.getMaxLevel() + " levels available.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("mLevels disabled.");
    }

    public void reloadAll() {
        reloadConfig();
        this.configManager.reload();
        this.levelManager.reload();
        this.kitManager.reload();
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
