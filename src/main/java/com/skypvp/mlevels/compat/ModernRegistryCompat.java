package com.skypvp.mlevels.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

/**
 * Uses the modern Registry + NamespacedKey API (1.20.5+, where Paper removed
 * the old static Enchantment/PotionEffectType constants). This class is only
 * ever touched from code paths guarded by {@link ServerVersion#hasModernRegistry()},
 * so the JVM never loads it on older servers where these symbols wouldn't
 * resolve at runtime - the class is package-private on purpose to keep that
 * boundary tight.
 */
final class ModernRegistryCompat {

    private ModernRegistryCompat() {}

    static Enchantment enchant(String minecraftKey) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(minecraftKey));
    }

    static PotionEffectType potionEffect(String minecraftKey) {
        return Registry.EFFECT.get(NamespacedKey.minecraft(minecraftKey));
    }

    static PotionType potionType(String minecraftKey) {
        return Registry.POTION.get(NamespacedKey.minecraft(minecraftKey));
    }
}
