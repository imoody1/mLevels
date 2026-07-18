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

        // Rank-kit command timing: "on-death" fires right here
        if ("on-death".equalsIgnoreCase(plugin.getConfigManager().getKitTiming())) {
            plugin.getKitManager().giveKitFor(victim);
        }

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
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());

        if (peakLevel <= 0) {
            // Brand new player - just give them the starter/rank kit, no progression gear yet
            plugin.getKitManager().giveKitFor(player);
        } else {
            // Returning player - make sure their earned progression gear is present
            Level level = plugin.getLevelManager().getLevel(peakLevel);
            if (level != null) {
                plugin.getLevelManager().applyGear(player, level);
            }
        }
    }

    /**
     * Order matters here:
     *   1) run the rank-kit command (base loadout from an external kits plugin)
     *   2) then re-apply the player's earned progression gear (sword/axe/
     *      armor matching their peak level) on top, so it always reflects
     *      their current level regardless of what the kit command gave.
     */
    private void handleRespawnGear(Player player) {
        int peakLevel = plugin.getPlayerDataManager().getPeakLevel(player.getUniqueId());

        if ("on-respawn".equalsIgnoreCase(plugin.getConfigManager().getKitTiming())) {
            plugin.getKitManager().giveKitFor(player);
        }

        if (peakLevel <= 0) {
            return; // no progression gear earned yet - the kit command above is all they get
        }

        Level level = plugin.getLevelManager().getLevel(peakLevel);
        if (level != null) {
            // Run one tick later so it always applies AFTER the kit command's
            // items have been given (kit commands can have their own delays).
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getLevelManager().applyGear(player, level), 2L);
        }
    }
}
