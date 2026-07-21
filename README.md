# mLevels - Progression / Leveling System for SkyPvP

Full config-driven leveling system: kill = +1 level, every level grants
exactly one upgrade or item reward, and gear is never lost on death (only
the visible level counter drops). Level 1 is a self-contained starter kit -
no external kits plugin needed.

## Version support: 1.8 - 1.21.11+
The plugin auto-detects the server's Minecraft version at runtime and picks
the correct API path for enchantments, potion effects, and items that
changed across versions:

- **1.20.5+**: uses the modern `Registry`/`NamespacedKey` enchantment and
  potion-effect system (the old static constants like `Enchantment.DAMAGE_ALL`
  were removed here).
- **1.8 - 1.20.4**: uses reflection on the classic static constant fields
  (`Enchantment.DAMAGE_ALL`, `PotionEffectType.INCREASE_DAMAGE`, etc).
- **Enchanted Golden Apple**: a separate `Material` only from 1.13 onward
  (the "flattening"); before that it's built as a regular `GOLDEN_APPLE`
  with the legacy data value that represents the enchanted variant.
- **Harming (tipped) arrows**: tipped arrows didn't exist before 1.9, so on
  those servers the final-level reward falls back to plain arrows instead
  (logged once so it's not a silent surprise).

This logic lives entirely in `com.skypvp.mlevels.compat` (`ServerVersion`,
`GearCompat`, and two internal resolver classes) - nothing elsewhere in the
plugin references version-specific API directly, so this is the only place
future Minecraft version changes would need updates. The jar is compiled to
Java 8 bytecode so it also loads on older server JVMs.

## Build
```bash
cd mLevels
mvn clean package
```
Output: `target/mLevels.jar`

## How leveling works
- **Level 1 is the default starter kit**: Leather armor (chestplate, leggings,
  helmet, boots), a Stone Sword, a Stone Axe, a plain Bow, and 1 Golden Apple.
  Every player starts here automatically on first join - no external kits
  plugin or permissions needed.
- Every level (1-119 by default) is **exactly one action**: give a weapon,
  give an armor piece, add an enchant level, give a potion, give items
  (golden apples / ender pearls / arrows), or a Thorns upgrade.
- The plugin tracks TWO numbers per player:
  - **level** - the visible counter. +1 per kill, can go down from death penalties.
  - **peak level** - the highest level ever reached. This is what actually
    drives your equipped gear. It NEVER decreases.
- On a kill: level goes up by 1. If that's a new peak, you get the real
  upgrade for that level. If you're just catching back up after a death
  penalty, the counter goes up but you don't get a duplicate reward (you
  already have that gear).
- At max level (119), every further kill just tops your golden apples /
  enchanted golden apples / arrows / Harming II arrows back up to simulate
  "unlimited" supply (see `progression.yml` -> level 119 -> `resupply-*`).

> Note: true infinite items aren't a real Minecraft mechanic, so "unlimited"
> here means auto-resupply on every kill once you're maxed out. Adjust the
> resupply amounts in `progression.yml` freely.

## Death system
- **Nothing drops on death.** All drops and dropped XP are cleared - no loot
  of any kind ends up on the ground, whether it's gear, apples, arrows, or
  anything else.
- A death penalty then reduces the **level counter** (never the gear/peak
  level) based on a formula in `config.yml` -> `death-penalty`:
  - peak level < 25: full reset (level and peak both go back to 0)
  - 25 - 34: lose 4 levels
  - 35 - 49: lose 5 levels
  - every 15 additional peak levels past 35 adds 1 more level to the loss
    (50-64 → 6, 65-79 → 7, ...)
- On respawn, the plugin always makes sure you have your earned sword/axe/
  armor back (matching your peak level), even though the counter dropped.
  This re-apply runs a few ticks after respawn/join at MONITOR priority, so
  it always applies last - if another plugin also touches the player's
  inventory on respawn, this plugin's gear wins.

## Commands
- `/level [player]` - shows current level / peak level
- `/mlevels reload` - reload configs (`mlevels.admin`)
- `/mlevels set <player> <level>` - set a player's level directly
- `/mlevels add <player> <amount>` - add levels manually
- `/mlevels remove <player> <amount>` - remove levels manually (counter only, gear stays)
- `/mlevels check [player]` - show level/peak/max
- `/mlevels reset <player>` - full reset (level, peak, and gear)

Aliases for the admin command: `/mlvl`, `/levelup`

## PlaceholderAPI
- `%mlevels_level%` - current level (drops on death)
- `%mlevels_peak%` - peak level (drives actual gear, never drops)
- `%mlevels_stage%` - current level's name
- `%mlevels_maxlevel%` - max level (119 by default)
- `%mlevels_progress%` - progress percentage based on peak level

## Config files
- `config.yml` - death-penalty tiers, broadcast toggle, all messages
- `progression.yml` - all 119 levels: action type, item/potion/enchant amounts,
  and the cumulative gear snapshot for that level. Every number is editable.

## Configuration notes
- Potion durations: "short" potions = 30s, "long" duration potions = 90s.
  Adjust these in `progression.yml`.
- Bow enchant progression skips Power I (goes straight from a plain bow at
  level 7 to Power II at level 19) - matches the intended progression order.
- "Unlimited" final rewards are implemented as auto-resupply on every kill
  past max level, topping stacks back up to configurable amounts.
- Death-penalty amounts are keyed off **peak level** (actual gear tier),
  not the fluctuating visible level counter, so the penalty always reflects
  how strong a player's gear really is.
- If you're testing on an account that already had progress from an older
  version of `progression.yml`, run `/mlevels reset <yourname>` after
  updating - old saved level numbers don't automatically remap if the
  level list itself changed.
