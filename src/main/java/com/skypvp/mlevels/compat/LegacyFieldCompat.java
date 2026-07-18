package com.skypvp.mlevels.compat;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;

/**
 * Resolves enchantments/potion effects on pre-1.20.5 servers using pure
 * reflection on the old static constant fields (Enchantment.DAMAGE_ALL,
 * PotionEffectType.INCREASE_DAMAGE, etc). These fields were removed from the
 * newest Paper API we compile against, so string-based reflection is used
 * instead of direct references - this still resolves correctly at runtime
 * against whatever Enchantment/PotionEffectType class the actual old server
 * provides, regardless of what we compiled against.
 */
final class LegacyFieldCompat {

    private LegacyFieldCompat() {}

    static Enchantment enchant(String legacyFieldName) {
        try {
            Field field = Enchantment.class.getField(legacyFieldName);
            return (Enchantment) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    static PotionEffectType potionEffect(String legacyFieldName) {
        try {
            Field field = PotionEffectType.class.getField(legacyFieldName);
            return (PotionEffectType) field.get(null);
        } catch (Exception e) {
            return null;
        }
    }
}
