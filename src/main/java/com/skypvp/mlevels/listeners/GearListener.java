package com.skypvp.mlevels.listeners;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.model.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class GearListener implements Listener {

    private final Main plugin;

    public GearListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        UUID victimId = victim.getUniqueId();

        // No loot at all on death - nothing drops, nothing is lootable.
        event.getDrops().clear();
        event.setDroppedExp(0);

        applyDeathPenalty(victim);

        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            levelUp(killer);
        }
    }

    private void applyDeathPenalty(Player victim) {
        UUID id = victim.getUniqueId();
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(id);

        if (plugin.getConfigManager().isFullResetLevel(peakLevel)) {
            plugin.getPlayerDataManager().setLevel(id, 0);
            plugin.getPlayerDataManager().setPeakLevel(id, 0);

            victim.sendMessage(plugin.getConfigManager().msg("death-penalty-reset"));
            return;
        }

        int currentLevel = plugin.getPlayerDataManager().getLevel(id);
        int penalty = plugin.getConfigManager().getDeathPenaltyForLevel(peakLevel);
        int newLevel = Math.max(0, currentLevel - penalty);

        plugin.getPlayerDataManager().setLevel(id, newLevel);

        victim.sendMessage(plugin.getConfigManager().msg("death-penalty")
                .replace("{amount}", String.valueOf(penalty))
                .replace("{level}", String.valueOf(newLevel)));
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Runs a few ticks late and at MONITOR priority on purpose: if another
        // plugin (a kits plugin, etc.) also gives items on respawn, ours
        // always applies last and wins, instead of getting overwritten.
        Bukkit.getScheduler().runTaskLater(plugin, () -> handleRespawnGear(player), 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> initializePlayerGear(player), 3L);
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
}
