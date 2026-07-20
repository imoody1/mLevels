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

    // Materials that must NEVER drop on death, regardless of enchants
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

        // Strip protected progression gear from the drop list - it must
        // never fall on the ground, no matter its level/enchants. Everything
        // else (apples, pearls, arrows, potions, food...) drops normally.
        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop != null && isProtectedGear(drop.getType())) {
                it.remove();
            }
        }

        // Apply the death penalty (reduces the visible level counter only -
        // the player's actual gear/peak-level is never rolled back).
        applyDeathPenalty(victim);

        // Give the killer a level up
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
        int currentLevel = plugin.getPlayerDataManager().getLevel(id);
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(id);

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
            // Already maxed out - just keep supplies topped up ("unlimited" simulation)
            plugin.getLevelManager().resupplyIfMaxLevel(killer);
            return;
        }

        int newLevel = currentLevel + 1;
        plugin.getPlayerDataManager().setLevel(killerId, newLevel);

        if (newLevel > peakLevel) {
            // Genuinely new ground - grant the real upgrade/reward for this level
            plugin.getPlayerDataManager().setPeakLevel(killerId, newLevel);
            Level level = plugin.getLevelManager().getLevel(newLevel);
            if (level != null) {
                plugin.getLevelManager().applyGear(killer, level);
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
            // Catching back up after a death penalty - no new reward, just informative message
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
            int nextLevel = plugin.getLevelManager().getNextLevelForGear(player);
            if (nextLevel > 1) {
                startFromExistingGear(player, nextLevel);
            } else {
                grantStarterKit(player);
            }
            return;
        }

        // Returning player with existing progress - just continue from
        // wherever they already are, never reset.
        Level level = plugin.getLevelManager().getLevel(peakLevel);
        if (level != null) {
            plugin.getLevelManager().applyGear(player, level);
        }
    }

    /**
     * Brand new player setup - level 1 in progression.yml IS the starter kit
     * (leather armor, wood sword/axe, plain bow, 1 golden apple). No rank
     * or permission checks - everyone starts the same and progresses purely
     * by killing.
     */
    public void grantStarterKit(Player player) {
        UUID id = player.getUniqueId();
        plugin.getPlayerDataManager().setLevel(id, 1);
        plugin.getPlayerDataManager().setPeakLevel(id, 1);

        Level starter = plugin.getLevelManager().getLevel(1);
        if (starter != null) {
            plugin.getLevelManager().applyGear(player, starter);
            plugin.getLevelManager().giveLevelReward(player, starter);
        }
    }

    private void startFromExistingGear(Player player, int nextLevel) {
        UUID id = player.getUniqueId();
        int currentLevel = Math.max(1, nextLevel - 1);
        plugin.getPlayerDataManager().setLevel(id, currentLevel);
        plugin.getPlayerDataManager().setPeakLevel(id, currentLevel);

        Level next = plugin.getLevelManager().getLevel(nextLevel);
        if (next != null) {
            plugin.getLevelManager().applyGear(player, next);
            plugin.getPlayerDataManager().setLevel(id, nextLevel);
            plugin.getPlayerDataManager().setPeakLevel(id, nextLevel);
            plugin.getLevelManager().giveLevelReward(player, next);
        }
    }

    private void handleRespawnGear(Player player) {
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());

        if (peakLevel <= 0) {
            // Safety net - should already be set to 1 on join, but never leave
            // a player with no gear at all.
            grantStarterKit(player);
            return;
        }

        Level level = plugin.getLevelManager().getLevel(peakLevel);
        if (level != null) {
            plugin.getLevelManager().applyGear(player, level);
        }

        giveRankKit(player);
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
