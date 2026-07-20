package com.skypvp.mlevels.managers;

import com.skypvp.mlevels.Main;
import com.skypvp.mlevels.compat.GearCompat;
import com.skypvp.mlevels.model.Level;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LevelManager {

    private final Main plugin;
    private File file;
    private FileConfiguration config;

    private final Map<Integer, Level> levels = new HashMap<>();
    private int maxLevel = 0;

    public LevelManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        levels.clear();
        load();
    }

    private void load() {
        file = new File(plugin.getDataFolder(), "progression.yml");
        if (!file.exists()) {
            plugin.saveResource("progression.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        maxLevel = config.getInt("max-level", 0);

        if (!config.isConfigurationSection("levels")) {
            plugin.getLogger().warning("progression.yml has no valid 'levels' section!");
            return;
        }

        for (String key : config.getConfigurationSection("levels").getKeys(false)) {
            try {
                int num = Integer.parseInt(key);
                String path = "levels." + key + ".";

                String name = config.getString(path + "name", "Level " + num);
                String actionRaw = config.getString(path + "action", "NONE");
                Level.Action action;
                try {
                    action = Level.Action.valueOf(actionRaw.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    action = Level.Action.FINAL;
                }

                Level level = new Level(num, name, action);

                level.setPiece(config.getString(path + "piece", ""));
                switch (action) {
                    case ARROWS:
                        level.setRewardAmount(config.getInt(path + "amount", 64));
                        break;
                    case ITEM:
                        level.setRewardMaterial(matOrNull(config.getString(path + "material")));
                        level.setRewardAmount(config.getInt(path + "amount", 1));
                        break;
                    case POTION:
                        level.setPotionEffectKey(config.getString(path + "effect", "SPEED"));
                        level.setPotionLevel(config.getInt(path + "level", 1));
                        level.setPotionDurationSeconds(config.getInt(path + "duration_seconds", 30));
                        break;
                    case FINAL:
                        level.setFinalGoldenApples(config.getInt(path + "golden_apples", 64));
                        level.setFinalEnchantedGoldenApples(config.getInt(path + "enchanted_golden_apples", 32));
                        level.setFinalHarmingArrowsLevel(config.getInt(path + "harming_arrows_level", 2));
                        level.setResupplyOnKill(config.getBoolean(path + "resupply-on-kill", true));
                        level.setResupplyGoldenApples(config.getInt(path + "resupply-golden-apples", 64));
                        level.setResupplyEnchantedGoldenApples(config.getInt(path + "resupply-enchanted-golden-apples", 8));
                        level.setResupplyArrows(config.getInt(path + "resupply-arrows", 64));
                        level.setResupplyHarmingArrows(config.getInt(path + "resupply-harming-arrows", 64));
                        break;
                    default:
                        break;
                }

                // Only read gear fields that are explicitly defined in the config
                String gearPath = path + "gear.";
                if (config.isSet(gearPath + "sword.material")) {
                    level.setSwordMaterial(matOrNull(config.getString(gearPath + "sword.material")));
                    level.setSwordSharpness(config.getInt(gearPath + "sword.sharpness", 0));
                }
                if (config.isSet(gearPath + "axe.material")) {
                    level.setAxeMaterial(matOrNull(config.getString(gearPath + "axe.material")));
                    level.setAxeSharpness(config.getInt(gearPath + "axe.sharpness", 0));
                }
                if (config.isSet(gearPath + "bow.power")) {
                    level.setBowPower(config.getInt(gearPath + "bow.power", 0));
                    level.setBowPunch(config.getInt(gearPath + "bow.punch", 0));
                }
                if (config.isSet(gearPath + "chestplate.material")) {
                    level.setChestMaterial(matOrNull(config.getString(gearPath + "chestplate.material")));
                    level.setChestProtection(config.getInt(gearPath + "chestplate.protection", 0));
                    level.setChestThorns(config.getInt(gearPath + "chestplate.thorns", 0));
                }
                if (config.isSet(gearPath + "leggings.material")) {
                    level.setLegsMaterial(matOrNull(config.getString(gearPath + "leggings.material")));
                    level.setLegsProtection(config.getInt(gearPath + "leggings.protection", 0));
                    level.setLegsThorns(config.getInt(gearPath + "leggings.thorns", 0));
                }
                if (config.isSet(gearPath + "helmet.material")) {
                    level.setHelmetMaterial(matOrNull(config.getString(gearPath + "helmet.material")));
                    level.setHelmetProtection(config.getInt(gearPath + "helmet.protection", 0));
                    level.setHelmetThorns(config.getInt(gearPath + "helmet.thorns", 0));
                }
                if (config.isSet(gearPath + "boots.material")) {
                    level.setBootsMaterial(matOrNull(config.getString(gearPath + "boots.material")));
                    level.setBootsProtection(config.getInt(gearPath + "boots.protection", 0));
                    level.setBootsThorns(config.getInt(gearPath + "boots.thorns", 0));
                }

                levels.put(num, level);
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid level number in progression.yml: " + key);
            }
        }

        plugin.getLogger().info("Loaded " + levels.size() + " levels from progression.yml (max-level: " + maxLevel + ")");
    }

    private Material matOrNull(String s) {
        if (s == null || s.isEmpty()) return null;
        return Material.matchMaterial(s.trim().toUpperCase());
    }

    public Level getLevel(int number) {
        return levels.get(number);
    }

    public int getMaxLevel() {
        return maxLevel > 0 ? maxLevel : levels.size();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void applyCumulativeGear(Player player, int upToLevel) {
        PlayerInventory inv = player.getInventory();

        removeItemType(inv, "_SWORD");
        removeItemType(inv, "_AXE");
        removeExactMaterial(inv, Material.BOW);
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);

        Material swordMat = null;
        int swordSharp = 0;
        Material axeMat = null;
        int axeSharp = 0;
        int bowPower = 0;
        int bowPunch = 0;
        Material chestMat = null;
        int chestProt = 0;
        int chestThorns = 0;
        Material legsMat = null;
        int legsProt = 0;
        int legsThorns = 0;
        Material helmMat = null;
        int helmProt = 0;
        int helmThorns = 0;
        Material bootsMat = null;
        int bootsProt = 0;
        int bootsThorns = 0;

        for (int i = 1; i <= upToLevel; i++) {
            Level lvl = getLevel(i);
            if (lvl == null) continue;

            if (lvl.getSwordMaterial() != null) {
                swordMat = lvl.getSwordMaterial();
                swordSharp = lvl.getSwordSharpness();
            }
            if (lvl.getAxeMaterial() != null) {
                axeMat = lvl.getAxeMaterial();
                axeSharp = lvl.getAxeSharpness();
            }
            if (lvl.getBowPower() > 0 || lvl.getBowPunch() > 0) {
                bowPower = lvl.getBowPower();
                bowPunch = lvl.getBowPunch();
            }
            if (lvl.getChestMaterial() != null) {
                chestMat = lvl.getChestMaterial();
                chestProt = lvl.getChestProtection();
                chestThorns = lvl.getChestThorns();
            }
            if (lvl.getLegsMaterial() != null) {
                legsMat = lvl.getLegsMaterial();
                legsProt = lvl.getLegsProtection();
                legsThorns = lvl.getLegsThorns();
            }
            if (lvl.getHelmetMaterial() != null) {
                helmMat = lvl.getHelmetMaterial();
                helmProt = lvl.getHelmetProtection();
                helmThorns = lvl.getHelmetThorns();
            }
            if (lvl.getBootsMaterial() != null) {
                bootsMat = lvl.getBootsMaterial();
                bootsProt = lvl.getBootsProtection();
                bootsThorns = lvl.getBootsThorns();
            }
        }

        if (swordMat != null) {
            ItemStack sword = new ItemStack(swordMat);
            if (swordSharp > 0) sword.addUnsafeEnchantment(GearCompat.sharpness(), swordSharp);
            inv.addItem(sword);
        }
        if (axeMat != null) {
            ItemStack axe = new ItemStack(axeMat);
            if (axeSharp > 0) axe.addUnsafeEnchantment(GearCompat.sharpness(), axeSharp);
            inv.addItem(axe);
        }
        if (helmMat != null) inv.setHelmet(buildArmor(helmMat, helmProt, helmThorns));
        if (chestMat != null) inv.setChestplate(buildArmor(chestMat, chestProt, chestThorns));
        if (legsMat != null) inv.setLeggings(buildArmor(legsMat, legsProt, legsThorns));
        if (bootsMat != null) inv.setBoots(buildArmor(bootsMat, bootsProt, bootsThorns));

        ItemStack bow = new ItemStack(Material.BOW);
        if (bowPower > 0) bow.addUnsafeEnchantment(GearCompat.power(), bowPower);
        if (bowPunch > 0) bow.addUnsafeEnchantment(GearCompat.punch(), bowPunch);
        inv.addItem(bow);
    }

    public void applyDeltaGear(Player player, Level level) {
        PlayerInventory inv = player.getInventory();

        if (level.getSwordMaterial() != null) {
            removeItemType(inv, "_SWORD");
            ItemStack sword = new ItemStack(level.getSwordMaterial());
            if (level.getSwordSharpness() > 0) sword.addUnsafeEnchantment(GearCompat.sharpness(), level.getSwordSharpness());
            inv.addItem(sword);
        }
        if (level.getAxeMaterial() != null) {
            removeItemType(inv, "_AXE");
            ItemStack axe = new ItemStack(level.getAxeMaterial());
            if (level.getAxeSharpness() > 0) axe.addUnsafeEnchantment(GearCompat.sharpness(), level.getAxeSharpness());
            inv.addItem(axe);
        }
        if (level.getChestMaterial() != null) {
            inv.setChestplate(buildArmor(level.getChestMaterial(), level.getChestProtection(), level.getChestThorns()));
        }
        if (level.getLegsMaterial() != null) {
            inv.setLeggings(buildArmor(level.getLegsMaterial(), level.getLegsProtection(), level.getLegsThorns()));
        }
        if (level.getHelmetMaterial() != null) {
            inv.setHelmet(buildArmor(level.getHelmetMaterial(), level.getHelmetProtection(), level.getHelmetThorns()));
        }
        if (level.getBootsMaterial() != null) {
            inv.setBoots(buildArmor(level.getBootsMaterial(), level.getBootsProtection(), level.getBootsThorns()));
        }
        if (level.getBowPower() > 0 || level.getBowPunch() > 0) {
            removeExactMaterial(inv, Material.BOW);
            ItemStack bow = new ItemStack(Material.BOW);
            if (level.getBowPower() > 0) bow.addUnsafeEnchantment(GearCompat.power(), level.getBowPower());
            if (level.getBowPunch() > 0) bow.addUnsafeEnchantment(GearCompat.punch(), level.getBowPunch());
            inv.addItem(bow);
        }
    }

    public void giveLevelReward(Player player, Level level) {
        PlayerInventory inv = player.getInventory();

        switch (level.getAction()) {
            case ARROWS:
                inv.addItem(new ItemStack(Material.ARROW, level.getRewardAmount()));
                break;
            case ITEM:
                if (level.getRewardMaterial() != null) {
                    inv.addItem(new ItemStack(level.getRewardMaterial(), level.getRewardAmount()));
                }
                break;
            case POTION:
                applyPotion(player, level.getPotionEffectKey(), level.getPotionLevel(), level.getPotionDurationSeconds());
                break;
            case FINAL:
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, level.getFinalGoldenApples()));
                inv.addItem(GearCompat.createEnchantedGoldenApple(level.getFinalEnchantedGoldenApples()));
                inv.addItem(GearCompat.createHarmingArrows(64, level.getFinalHarmingArrowsLevel(), plugin.getLogger()));
                break;
            default:
                break;
        }
    }

    public void applyGear(Player player, Level level) {
        applyCumulativeGear(player, level.getNumber());
    }

    public int getNextLevelForGear(Player player) {
        // Scan player's gear and find the highest matching level
        PlayerInventory inv = player.getInventory();
        int best = 0;
        for (int i = 1; i <= getMaxLevel(); i++) {
            Level lvl = getLevel(i);
            if (lvl == null) continue;
            if (lvl.getSwordMaterial() != null) {
                ItemStack sword = getItemBySuffix(inv, "_SWORD");
                if (sword == null || sword.getType() != lvl.getSwordMaterial()) continue;
            }
            if (lvl.getHelmetMaterial() != null) {
                ItemStack helm = inv.getHelmet();
                if (helm == null || helm.getType() != lvl.getHelmetMaterial()) continue;
            }
            if (lvl.getChestMaterial() != null) {
                ItemStack chest = inv.getChestplate();
                if (chest == null || chest.getType() != lvl.getChestMaterial()) continue;
            }
            if (lvl.getLegsMaterial() != null) {
                ItemStack legs = inv.getLeggings();
                if (legs == null || legs.getType() != lvl.getLegsMaterial()) continue;
            }
            if (lvl.getBootsMaterial() != null) {
                ItemStack boots = inv.getBoots();
                if (boots == null || boots.getType() != lvl.getBootsMaterial()) continue;
            }
            best = i;
        }
        return best + 1;
    }

    private ItemStack getItemBySuffix(PlayerInventory inv, String suffix) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType().name().endsWith(suffix)) return item;
        }
        return null;
    }

    public void resupplyIfMaxLevel(Player player) {
        Level finalLevel = getLevel(getMaxLevel());
        if (finalLevel == null || finalLevel.getAction() != Level.Action.FINAL || !finalLevel.isResupplyOnKill()) return;

        PlayerInventory inv = player.getInventory();
        topUp(inv, Material.GOLDEN_APPLE, finalLevel.getResupplyGoldenApples());
        Material enchantedApple = Material.matchMaterial("ENCHANTED_GOLDEN_APPLE");
        if (enchantedApple != null) topUp(inv, enchantedApple, finalLevel.getResupplyEnchantedGoldenApples());
        topUp(inv, Material.ARROW, finalLevel.getResupplyArrows());
        topUpHarmingArrows(inv, finalLevel.getFinalHarmingArrowsLevel(), finalLevel.getResupplyHarmingArrows());
    }

    private void topUp(PlayerInventory inv, Material material, int targetAmount) {
        int current = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) current += item.getAmount();
        }
        if (current < targetAmount) {
            inv.addItem(new ItemStack(material, targetAmount - current));
        }
    }

    private void topUpHarmingArrows(PlayerInventory inv, int harmingLevel, int targetAmount) {
        Material tipped = Material.matchMaterial("TIPPED_ARROW");
        if (tipped == null) return;

        int current = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == tipped) current += item.getAmount();
        }
        if (current < targetAmount) {
            inv.addItem(GearCompat.createHarmingArrows(targetAmount - current, harmingLevel, plugin.getLogger()));
        }
    }

    private void applyPotion(Player player, String effectKey, int level, int durationSeconds) {
        if (effectKey == null) return;
        String key = effectKey.toLowerCase();
        PotionEffectType type;
        switch (key) {
            case "speed": type = GearCompat.speed(); break;
            case "strength": type = GearCompat.strength(); break;
            case "jump_boost": type = GearCompat.jumpBoost(); break;
            default: type = GearCompat.resolveArbitraryPotionEffect(key); break;
        }
        if (type == null) return;
        player.addPotionEffect(new PotionEffect(type, durationSeconds * 20, Math.max(0, level - 1), true, true));
    }

    private ItemStack buildArmor(Material material, int protectionLevel, int thornsLevel) {
        ItemStack item = new ItemStack(material);
        if (protectionLevel > 0) item.addUnsafeEnchantment(GearCompat.protection(), protectionLevel);
        if (thornsLevel > 0) item.addUnsafeEnchantment(GearCompat.thorns(), thornsLevel);
        return item;
    }

    private void removeItemType(PlayerInventory inv, String suffix) {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && it.getType().name().endsWith(suffix)) inv.setItem(i, null);
        }
    }

    private void removeExactMaterial(PlayerInventory inv, Material material) {
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && it.getType() == material) inv.setItem(i, null);
        }
    }
}