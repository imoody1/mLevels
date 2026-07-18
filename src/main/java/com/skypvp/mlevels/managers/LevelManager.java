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

                // cumulative gear snapshot - always present
                level.setSwordMaterial(matOrDefault(config.getString(path + "gear.sword.material"), Material.IRON_SWORD, "SWORD"));
                level.setSwordSharpness(config.getInt(path + "gear.sword.sharpness", 0));
                level.setAxeMaterial(matOrDefault(config.getString(path + "gear.axe.material"), Material.IRON_AXE, "AXE"));
                level.setAxeSharpness(config.getInt(path + "gear.axe.sharpness", 0));
                level.setBowPower(config.getInt(path + "gear.bow.power", 0));
                level.setBowPunch(config.getInt(path + "gear.bow.punch", 0));

                level.setChestMaterial(matOrDefault(config.getString(path + "gear.chestplate.material"), Material.IRON_CHESTPLATE, "CHESTPLATE"));
                level.setChestProtection(config.getInt(path + "gear.chestplate.protection", 0));
                level.setChestThorns(config.getInt(path + "gear.chestplate.thorns", 0));

                level.setLegsMaterial(matOrDefault(config.getString(path + "gear.leggings.material"), Material.IRON_LEGGINGS, "LEGGINGS"));
                level.setLegsProtection(config.getInt(path + "gear.leggings.protection", 0));
                level.setLegsThorns(config.getInt(path + "gear.leggings.thorns", 0));

                level.setHelmetMaterial(matOrDefault(config.getString(path + "gear.helmet.material"), Material.IRON_HELMET, "HELMET"));
                level.setHelmetProtection(config.getInt(path + "gear.helmet.protection", 0));
                level.setHelmetThorns(config.getInt(path + "gear.helmet.thorns", 0));

                level.setBootsMaterial(matOrDefault(config.getString(path + "gear.boots.material"), Material.IRON_BOOTS, "BOOTS"));
                level.setBootsProtection(config.getInt(path + "gear.boots.protection", 0));
                level.setBootsThorns(config.getInt(path + "gear.boots.thorns", 0));

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

    private Material matOrDefault(String rawMaterialTier, Material fallback, String pieceType) {
        if (rawMaterialTier == null) return fallback;
        Material m = Material.matchMaterial(rawMaterialTier.trim().toUpperCase() + "_" + pieceType);
        return m != null ? m : fallback;
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

    /**
     * Applies the gear snapshot for the given level (sword/axe/armor/bow) to the
     * player, replacing whatever they currently have equipped in those slots.
     * This does NOT touch consumables (apples/pearls/potions/arrows) - those
     * are only granted once, at the moment the level is first reached
     * (see {@link #giveLevelReward}).
     */
    public void applyGear(Player player, Level level) {
        PlayerInventory inv = player.getInventory();

        removeItemType(inv, "_SWORD");
        removeItemType(inv, "_AXE");
        removeExactMaterial(inv, Material.BOW);

        ItemStack sword = new ItemStack(level.getSwordMaterial());
        if (level.getSwordSharpness() > 0) sword.addUnsafeEnchantment(GearCompat.sharpness(), level.getSwordSharpness());
        inv.addItem(sword);

        ItemStack axe = new ItemStack(level.getAxeMaterial());
        if (level.getAxeSharpness() > 0) axe.addUnsafeEnchantment(GearCompat.sharpness(), level.getAxeSharpness());
        inv.addItem(axe);

        inv.setHelmet(buildArmor(level.getHelmetMaterial(), level.getHelmetProtection(), level.getHelmetThorns()));
        inv.setChestplate(buildArmor(level.getChestMaterial(), level.getChestProtection(), level.getChestThorns()));
        inv.setLeggings(buildArmor(level.getLegsMaterial(), level.getLegsProtection(), level.getLegsThorns()));
        inv.setBoots(buildArmor(level.getBootsMaterial(), level.getBootsProtection(), level.getBootsThorns()));

        ItemStack bow = new ItemStack(Material.BOW);
        if (level.getBowPower() > 0) bow.addUnsafeEnchantment(GearCompat.power(), level.getBowPower());
        if (level.getBowPunch() > 0) bow.addUnsafeEnchantment(GearCompat.punch(), level.getBowPunch());
        inv.addItem(bow);
    }

    /**
     * Grants the one-time reward tied to reaching this level (item / arrows /
     * potion / final bundle). Gear-related actions (GIVE_WEAPON, GIVE_ARMOR,
     * GIVE_BOW, enchant actions, thorns) don't need a separate reward call
     * since applyGear already reflects them.
     */
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

    /**
     * Called on every kill once a player is already at max level, to simulate
     * "unlimited" supplies by topping stacks back up (see progression.yml ->
     * final level -> resupply-* settings).
     */
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
        if (tipped == null) return; // pre-1.9 server - nothing to top up

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
