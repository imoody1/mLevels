package com.skypvp.mlevels.listeners;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.model.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.UUID;

public class GearListener implements Listener {

    private static final String[] PROTECTED_SUFFIXES = {
            "_SWORD", "_AXE", "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS"
    };

    private final Main plugin;

    public GearListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UUID victimId = victim.getUniqueId();

        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop != null && isProtectedGear(drop.getType())) {
                it.remove();
            }
        }

        applyDeathPenalty(victim);

        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            levelUp(killer);
        }
    }

    private boolean isProtectedGear(Material material) {
        String name = material.name();
        for (String suffix : PROTECTED_SUFFIXES) {
            if (name.endsWith(suffix)) return true;
        }
        return false;
    }

    private void applyDeathPenalty(Player victim) {
        UUID id = victim.getUniqueId();
        plugin.getPlayerDataManager().setLevel(id, 0);
        plugin.getPlayerDataManager().setPeakLevel(id, 0);

        victim.sendMessage(plugin.getConfigManager().msg("death-penalty")
                .replace("{amount}", "all")
                .replace("{level}", "0"));
    }

    private void levelUp(Player killer) {
        UUID killerId = killer.getUniqueId();
        int currentLevel = plugin.getPlayerDataManager().getLevel(killerId);
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(killerId);
        int maxLevel = plugin.getLevelManager().getMaxLevel();

        if (currentLevel >= maxLevel) {
            plugin.getLevelManager().resupplyIfMaxLevel(killer);
            return;
        }

        int newLevel = currentLevel + 1;
        plugin.getPlayerDataManager().setLevel(killerId, newLevel);

        if (newLevel > peakLevel) {
            plugin.getPlayerDataManager().setPeakLevel(killerId, newLevel);
            Level level = plugin.getLevelManager().getLevel(newLevel);
            if (level != null) {
                plugin.getLevelManager().applyDeltaGear(killer, level);
                plugin.getLevelManager().giveLevelReward(killer, level);

                killer.sendMessage(plugin.getConfigManager().msg("level-up")
                        .replace("{player}", killer.getName())
                        .replace("{stage}", level.getName())
                        .replace("{level}", String.valueOf(newLevel))
                        .replace("{max_level}", String.valueOf(maxLevel)));

                if (plugin.getConfigManager().isBroadcastLevelUp()) {
                    Bukkit.broadcastMessage(plugin.getConfigManager().msg("level-up-broadcast")
                            .replace("{player}", killer.getName())
                            .replace("{stage}", level.getName())
                            .replace("{level}", String.valueOf(newLevel))
                            .replace("{max_level}", String.valueOf(maxLevel)));
                }
            }
        } else {
            killer.sendMessage(plugin.getConfigManager().msg("level-restored")
                    .replace("{level}", String.valueOf(newLevel))
                    .replace("{max_level}", String.valueOf(maxLevel)));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> handleRespawnGear(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> initializePlayerGear(player));
    }

    private void initializePlayerGear(Player player) {
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());

        if (peakLevel <= 0) {
            grantStarterKit(player);
        } else {
            int currentLevel = plugin.getPlayerDataManager().getLevel(player.getUniqueId());
            plugin.getLevelManager().applyCumulativeGear(player, Math.max(currentLevel, peakLevel));
        }
    }

    public void grantStarterKit(Player player) {
        UUID id = player.getUniqueId();
        plugin.getPlayerDataManager().setLevel(id, 1);
        plugin.getPlayerDataManager().setPeakLevel(id, 1);

        Level starter = plugin.getLevelManager().getLevel(1);
        if (starter != null) {
            plugin.getLevelManager().applyCumulativeGear(player, 1);
            plugin.getLevelManager().giveLevelReward(player, starter);
        }
    }

    private void handleRespawnGear(Player player) {
        int currentLevel = plugin.getPlayerDataManager().getLevel(player.getUniqueId());
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());

        if (peakLevel <= 0) {
            grantStarterKit(player);
        } else {
            plugin.getLevelManager().applyCumulativeGear(player, Math.max(currentLevel, peakLevel));
        }
    }

    private void giveRankKit(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerKits2")
                || !plugin.getConfig().getBoolean("rank-kits.enabled", true)) {
            return;
        }

        String[] ranks = {"custom", "revon", "turbo", "elite", "vip"};
        for (String rank : ranks) {
            String permission = plugin.getConfig().getString("rank-kits.ranks." + rank + ".permission", "group." + rank);
            if (permission == null || !player.hasPermission(permission)) {
                continue;
            }

            String kit = plugin.getConfig().getString("rank-kits.ranks." + rank + ".kit", rank);
            String command = plugin.getConfig().getString("rank-kits.command", "playerkits2 claim {kit}")
                    .replace("{kit}", kit == null ? rank : kit);
            player.performCommand(command);
            return;
        }
    }
}