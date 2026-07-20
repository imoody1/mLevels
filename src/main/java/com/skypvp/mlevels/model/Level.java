package com.skypvp.mlevels.model;

import org.bukkit.Material;

/**
 * Represents a single level: one action/reward, plus the cumulative gear
 * snapshot the player should have equipped once they reach this level.
 */
public class Level {

    public enum Action {
        GIVE_WEAPON, GIVE_ARMOR, GIVE_BOW, ARROWS, ITEM,
        ENCHANT_SWORD, ENCHANT_AXE, ENCHANT_ARMOR, BOW_ENCHANT,
        POTION, THORNS, FINAL
    }

    private final int number;
    private final String name;
    private final Action action;

    // action-specific fields (only relevant ones are populated depending on Action)
    private String piece;
    private Material rewardMaterial;
    private int rewardAmount;
    private String potionEffectKey;   // e.g. "jump_boost", "strength", "speed"
    private int potionLevel;
    private int potionDurationSeconds;

    // final-level rewards
    private int finalGoldenApples;
    private int finalEnchantedGoldenApples;
    private int finalHarmingArrowsLevel;
    private boolean resupplyOnKill;
    private int resupplyGoldenApples;
    private int resupplyEnchantedGoldenApples;
    private int resupplyArrows;
    private int resupplyHarmingArrows;

    // cumulative gear snapshot
    private Material swordMaterial;
    private int swordSharpness;
    private Material axeMaterial;
    private int axeSharpness;
    private int bowPower;
    private int bowPunch;
    private Material chestMaterial;
    private int chestProtection;
    private int chestThorns;
    private Material legsMaterial;
    private int legsProtection;
    private int legsThorns;
    private Material helmetMaterial;
    private int helmetProtection;
    private int helmetThorns;
    private Material bootsMaterial;
    private int bootsProtection;
    private int bootsThorns;

    public Level(int number, String name, Action action) {
        this.number = number;
        this.name = name;
        this.action = action;
    }

    public String getPiece() { return piece; }
    public void setPiece(String piece) { this.piece = piece; }

    public int getNumber() { return number; }
    public String getName() { return name; }
    public Action getAction() { return action; }

    public Material getRewardMaterial() { return rewardMaterial; }
    public void setRewardMaterial(Material m) { this.rewardMaterial = m; }
    public int getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(int a) { this.rewardAmount = a; }

    public String getPotionEffectKey() { return potionEffectKey; }
    public void setPotionEffectKey(String k) { this.potionEffectKey = k; }
    public int getPotionLevel() { return potionLevel; }
    public void setPotionLevel(int l) { this.potionLevel = l; }
    public int getPotionDurationSeconds() { return potionDurationSeconds; }
    public void setPotionDurationSeconds(int d) { this.potionDurationSeconds = d; }

    public int getFinalGoldenApples() { return finalGoldenApples; }
    public void setFinalGoldenApples(int v) { this.finalGoldenApples = v; }
    public int getFinalEnchantedGoldenApples() { return finalEnchantedGoldenApples; }
    public void setFinalEnchantedGoldenApples(int v) { this.finalEnchantedGoldenApples = v; }
    public int getFinalHarmingArrowsLevel() { return finalHarmingArrowsLevel; }
    public void setFinalHarmingArrowsLevel(int v) { this.finalHarmingArrowsLevel = v; }
    public boolean isResupplyOnKill() { return resupplyOnKill; }
    public void setResupplyOnKill(boolean v) { this.resupplyOnKill = v; }
    public int getResupplyGoldenApples() { return resupplyGoldenApples; }
    public void setResupplyGoldenApples(int v) { this.resupplyGoldenApples = v; }
    public int getResupplyEnchantedGoldenApples() { return resupplyEnchantedGoldenApples; }
    public void setResupplyEnchantedGoldenApples(int v) { this.resupplyEnchantedGoldenApples = v; }
    public int getResupplyArrows() { return resupplyArrows; }
    public void setResupplyArrows(int v) { this.resupplyArrows = v; }
    public int getResupplyHarmingArrows() { return resupplyHarmingArrows; }
    public void setResupplyHarmingArrows(int v) { this.resupplyHarmingArrows = v; }

    public Material getSwordMaterial() { return swordMaterial; }
    public void setSwordMaterial(Material m) { this.swordMaterial = m; }
    public int getSwordSharpness() { return swordSharpness; }
    public void setSwordSharpness(int v) { this.swordSharpness = v; }
    public Material getAxeMaterial() { return axeMaterial; }
    public void setAxeMaterial(Material m) { this.axeMaterial = m; }
    public int getAxeSharpness() { return axeSharpness; }
    public void setAxeSharpness(int v) { this.axeSharpness = v; }
    public int getBowPower() { return bowPower; }
    public void setBowPower(int v) { this.bowPower = v; }
    public int getBowPunch() { return bowPunch; }
    public void setBowPunch(int v) { this.bowPunch = v; }

    public Material getChestMaterial() { return chestMaterial; }
    public void setChestMaterial(Material m) { this.chestMaterial = m; }
    public int getChestProtection() { return chestProtection; }
    public void setChestProtection(int v) { this.chestProtection = v; }
    public int getChestThorns() { return chestThorns; }
    public void setChestThorns(int v) { this.chestThorns = v; }

    public Material getLegsMaterial() { return legsMaterial; }
    public void setLegsMaterial(Material m) { this.legsMaterial = m; }
    public int getLegsProtection() { return legsProtection; }
    public void setLegsProtection(int v) { this.legsProtection = v; }
    public int getLegsThorns() { return legsThorns; }
    public void setLegsThorns(int v) { this.legsThorns = v; }

    public Material getHelmetMaterial() { return helmetMaterial; }
    public void setHelmetMaterial(Material m) { this.helmetMaterial = m; }
    public int getHelmetProtection() { return helmetProtection; }
    public void setHelmetProtection(int v) { this.helmetProtection = v; }
    public int getHelmetThorns() { return helmetThorns; }
    public void setHelmetThorns(int v) { this.helmetThorns = v; }

    public Material getBootsMaterial() { return bootsMaterial; }
    public void setBootsMaterial(Material m) { this.bootsMaterial = m; }
    public int getBootsProtection() { return bootsProtection; }
    public void setBootsProtection(int v) { this.bootsProtection = v; }
    public int getBootsThorns() { return bootsThorns; }
    public void setBootsThorns(int v) { this.bootsThorns = v; }

    public boolean hasAnyGear() {
        return swordMaterial != null || axeMaterial != null || bowPower > 0 || bowPunch > 0
                || chestMaterial != null || legsMaterial != null || helmetMaterial != null || bootsMaterial != null;
    }
}
