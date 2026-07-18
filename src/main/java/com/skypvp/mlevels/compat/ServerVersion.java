package com.skypvp.mlevels.compat;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the running server's Minecraft version so the rest of the
 * compatibility layer can pick the right API path (1.8 through 1.21.11+).
 */
public final class ServerVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private static int MAJOR;
    private static int MINOR;
    private static int PATCH;

    static {
        parse();
    }

    private ServerVersion() {}

    private static void parse() {
        String raw = Bukkit.getBukkitVersion(); // e.g. "1.21.11-R0.1-SNAPSHOT" or "1.8.8-R0.1-SNAPSHOT"
        Matcher m = VERSION_PATTERN.matcher(raw);
        if (m.find()) {
            MAJOR = Integer.parseInt(m.group(1));
            MINOR = Integer.parseInt(m.group(2));
            PATCH = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        } else {
            // Fallback: assume modern if we somehow can't parse it
            MAJOR = 1;
            MINOR = 21;
            PATCH = 0;
        }
    }

    public static int minor() { return MINOR; }
    public static int patch() { return PATCH; }

    /** True on 1.(minor).(patch) or newer, assuming major is always 1. */
    public static boolean isAtLeast(int minor, int patch) {
        if (MINOR != minor) return MINOR > minor;
        return PATCH >= patch;
    }

    /** 1.20.5+ - Paper removed the legacy Enchantment/PotionEffectType static fields here. */
    public static boolean hasModernRegistry() {
        return isAtLeast(20, 5);
    }

    /** 1.13+ - the "flattening": ENCHANTED_GOLDEN_APPLE became its own Material. */
    public static boolean hasFlattenedMaterials() {
        return isAtLeast(13, 0);
    }

    /** 1.9+ - tipped/lingering arrows and the modern potion-on-arrow system. */
    public static boolean hasTippedArrows() {
        return isAtLeast(9, 0);
    }

    public static String describe() {
        return "1." + MINOR + "." + PATCH;
    }
}
