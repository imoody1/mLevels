package com.skypvp.mlevels.compat;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Single entry point the rest of the plugin uses for anything that differs
 * between Minecraft 1.8 and 1.21.11+: enchantments, potion effects, the
 * enchanted golden apple item, and harming (tipped) arrows.
 *
 * Chooses the modern Registry-based path on 1.20.5+ and falls back to
 * reflection on the old static constant fields everywhere else, so a single
 * compiled jar works across the whole range without needing separate builds.
 */
public final class GearCompat {

    private GearCompat() {}

    // modern registry key -> legacy static field name
    private static final Map<String, String> ENCHANT_LEGACY_NAMES = new HashMap<>();
    private static final Map<String, String> POTION_LEGACY_NAMES = new HashMap<>();

    static {
        ENCHANT_LEGACY_NAMES.put("sharpness", "DAMAGE_ALL");
        ENCHANT_LEGACY_NAMES.put("protection", "PROTECTION_ENVIRONMENTAL");
        ENCHANT_LEGACY_NAMES.put("power", "ARROW_DAMAGE");
        ENCHANT_LEGACY_NAMES.put("punch", "ARROW_KNOCKBACK");
        ENCHANT_LEGACY_NAMES.put("thorns", "THORNS");

        POTION_LEGACY_NAMES.put("speed", "SPEED");
        POTION_LEGACY_NAMES.put("strength", "INCREASE_DAMAGE");
        POTION_LEGACY_NAMES.put("jump_boost", "JUMP");
    }

    private static final Map<String, Enchantment> ENCHANT_CACHE = new HashMap<>();
    private static final Map<String, PotionEffectType> POTION_CACHE = new HashMap<>();

    public static Enchantment sharpness() { return resolveEnchant("sharpness"); }
    public static Enchantment protection() { return resolveEnchant("protection"); }
    public static Enchantment power() { return resolveEnchant("power"); }
    public static Enchantment punch() { return resolveEnchant("punch"); }
    public static Enchantment thorns() { return resolveEnchant("thorns"); }

    public static PotionEffectType speed() { return resolvePotionEffect("speed"); }
    public static PotionEffectType strength() { return resolvePotionEffect("strength"); }
    public static PotionEffectType jumpBoost() { return resolvePotionEffect("jump_boost"); }

    /** Resolves an arbitrary potion effect by its modern minecraft key (e.g. "regeneration"). */
    public static PotionEffectType resolveArbitraryPotionEffect(String modernKey) {
        return resolvePotionEffect(modernKey.toLowerCase());
    }

    private static Enchantment resolveEnchant(String modernKey) {
        return ENCHANT_CACHE.computeIfAbsent(modernKey, key -> {
            if (ServerVersion.hasModernRegistry()) {
                Enchantment e = ModernRegistryCompat.enchant(key);
                if (e != null) return e;
            }
            String legacyName = ENCHANT_LEGACY_NAMES.get(key);
            return legacyName != null ? LegacyFieldCompat.enchant(legacyName) : null;
        });
    }

    private static PotionEffectType resolvePotionEffect(String modernKey) {
        return POTION_CACHE.computeIfAbsent(modernKey, key -> {
            if (ServerVersion.hasModernRegistry()) {
                PotionEffectType t = ModernRegistryCompat.potionEffect(key);
                if (t != null) return t;
            }
            String legacyName = POTION_LEGACY_NAMES.getOrDefault(key, key.toUpperCase());
            PotionEffectType t = LegacyFieldCompat.potionEffect(legacyName);
            if (t != null) return t;
            // last resort for keys with no legacy mapping registered above
            return LegacyFieldCompat.potionEffect(key.toUpperCase());
        });
    }

    /**
     * Builds a stack of enchanted golden apples. Pre-1.13 servers don't have
     * a separate ENCHANTED_GOLDEN_APPLE material - it's GOLDEN_APPLE with
     * data value 1, set through the legacy setDurability(short) method.
     */
    public static ItemStack createEnchantedGoldenApple(int amount) {
        if (ServerVersion.hasFlattenedMaterials()) {
            Material mat = Material.matchMaterial("ENCHANTED_GOLDEN_APPLE");
            if (mat != null) return new ItemStack(mat, amount);
        }
        ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, amount);
        try {
            Method setDurability = ItemStack.class.getMethod("setDurability", short.class);
            setDurability.invoke(apple, (short) 1);
        } catch (Exception ignored) {
            // if even that fails, the player still gets a regular golden apple - never crash here
        }
        return apple;
    }

    /**
     * Builds tipped arrows with a Harming effect. Tipped arrows don't exist
     * before 1.9 - on those servers this silently returns plain arrows
     * instead, since there's no vanilla item to represent it.
     */
    public static ItemStack createHarmingArrows(int amount, int harmingLevel, Logger logger) {
        if (!ServerVersion.hasTippedArrows()) {
            if (logger != null) {
                logger.warning("Harming arrows require 1.9+ (tipped arrows didn't exist before that) - giving regular arrows instead on this server (" + ServerVersion.describe() + ").");
            }
            return new ItemStack(Material.ARROW, amount);
        }

        Material tipped = Material.matchMaterial("TIPPED_ARROW");
        if (tipped == null) return new ItemStack(Material.ARROW, amount);

        ItemStack arrows = new ItemStack(tipped, amount);
        ItemMeta meta = arrows.getItemMeta();
        if (!(meta instanceof PotionMeta)) return arrows;
        PotionMeta potionMeta = (PotionMeta) meta;

        boolean strong = harmingLevel >= 2;

        // Try the modern API first (setBasePotionType(PotionType), 1.20.2+)
        if (trySetBasePotionType(potionMeta, strong)) {
            arrows.setItemMeta(potionMeta);
            return arrows;
        }
        // Fall back to the older PotionData-based API (1.9 - 1.20.1)
        if (trySetBasePotionData(potionMeta, strong)) {
            arrows.setItemMeta(potionMeta);
            return arrows;
        }

        arrows.setItemMeta(potionMeta);
        return arrows;
    }

    private static boolean trySetBasePotionType(PotionMeta meta, boolean strong) {
        try {
            Class<?> potionTypeClass = Class.forName("org.bukkit.potion.PotionType");
            Object type;
            if (ServerVersion.hasModernRegistry()) {
                type = strong ? ModernRegistryCompat.potionType("strong_harming") : ModernRegistryCompat.potionType("harming");
            } else {
                String fieldName = strong ? "STRONG_HARMING" : "HARMING";
                Object fromField = safeGetStaticField(potionTypeClass, fieldName);
                type = fromField != null ? fromField : safeGetStaticField(potionTypeClass, "INSTANT_DAMAGE");
            }
            if (type == null) return false;

            Method setBasePotionType = PotionMeta.class.getMethod("setBasePotionType", potionTypeClass);
            setBasePotionType.invoke(meta, type);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean trySetBasePotionData(PotionMeta meta, boolean strong) {
        try {
            Class<?> potionDataClass = Class.forName("org.bukkit.potion.PotionData");
            Class<?> potionTypeClass = Class.forName("org.bukkit.potion.PotionType");
            Object instantDamage = safeGetStaticField(potionTypeClass, "INSTANT_DAMAGE");
            if (instantDamage == null) return false;

            Object potionData = potionDataClass
                    .getConstructor(potionTypeClass, boolean.class, boolean.class)
                    .newInstance(instantDamage, false, strong);

            Method setBasePotionData = PotionMeta.class.getMethod("setBasePotionData", potionDataClass);
            setBasePotionData.invoke(meta, potionData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Object safeGetStaticField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
