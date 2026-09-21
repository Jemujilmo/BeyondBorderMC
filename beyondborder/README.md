# Beyond the Border (Fabric, Minecraft 1.21.6)

An experiment mod. It removes the world border and the hard ±30,000,000-block coordinate limits, and lets you set a new limit in a config file, so you can travel far past 30 million and watch what breaks.

**Status: not yet run against a real 1.21.6 game.** It was written and type-checked without access to the Minecraft jar. Every patch target was checked against the 1.21.6 Yarn mappings, and the patch points match ones a published mod (BorderRemover, MIT) uses on both 1.20.1 and 1.21.11. Expect it to work, but if a mixin fails to apply, see "If it fails at startup" below.

## What it changes

| Vanilla limit | What the mod does |
|---|---|
| World border (max 5.9999968E7 wide) | Border edges report ±limit, no collision wall, no damage |
| `World.isValidHorizontally` (±30,000,000 block check; blocks `setBlock` and `/tp`) | Uses ±limit |
| Server clamp on movement packets (`clampHorizontal`) | Clamps to ±limit |
| `Entity.updatePosition` / `Entity.readData` clamps (teleports, spawns, loading saves) | Clamp to ±limit |
| `PlayerEntity.tick` clamp (pulls you back to ±29,999,999 each tick) | Clamps to ±limit |

Vertical limits are untouched. The mod must be installed on the client and the server; a vanilla client still sees the vanilla border.

## Config: `config/beyondborder.json`

Created on first launch.

```json
{
  "limit": 1.0E12,
  "disabledMixins": []
}
```

`limit` is the half-width of the playable square, in blocks. Values that make the experiment interesting:

| Coordinate | What is expected to happen |
|---|---|
| 1.6777216E7 (2^24) | 32-bit floats can no longer represent every whole number (float step here is 2 blocks) |
| 3.0E7 | Vanilla limit |
| 3.3554432E7 (2^25) | `BlockPos` and chunk-section packing wraps: block keys start aliasing places on the opposite side of the world |
| 2.147483647E9 (2^31) | Block coordinates overflow a 32-bit int |
| ~2.25E15 (2^51) | Doubles get coarser than a walking step, so you stop being able to move |
| ~9.0E15 (2^53) | Doubles can no longer resolve a whole-block step |

## Build

You need JDK 21.

```
./gradlew build          # Windows: gradlew.bat build
```

The jar to use is `build/libs/beyondborder-1.0.0.jar` (not the `-sources` one). To test in a dev client, run `./gradlew runClient`.

No Java installed? Push this folder to a GitHub repository. The included workflow (`.github/workflows/build.yml`) builds the jar for you; download it from the run's Artifacts.

## Install with the CurseForge app

CurseForge is a launcher and mod host, not a mod loader, so this runs as a normal Fabric mod:

1. Create a **custom profile**: Minecraft **1.21.6**, mod loader **Fabric** (any current Fabric Loader).
2. Add **Fabric API** for 1.21.6 from the app's mod browser.
3. Open the profile folder, then `mods`, and drop `beyondborder-1.0.0.jar` in.

(Publishing it as a CurseForge project is a separate step through an author account; it isn't needed to use it yourself.)

## Trying it

Use a throwaway creative world. Region files for chunks at absurd coordinates are strange, and you don't want that in a world you care about.

1. `/beyondborder` shows the world border bound. It should say `[patched]` with the value from your config; if it shows 2.9999984E7 the border mixin didn't apply. It also reports how far past vanilla you are, whether block positions have wrapped, and the float and double step size at your X.
2. `/tp @s 29999990 100 0`, then walk or fly east across the old limit.
3. `/tp @s 35000000 100 0`, then run `/beyondborder` again and look for `[WRAPPED]`.
4. Push further with bigger numbers. Type them out in full (`/tp @s 2147483000 100 0`): Minecraft's command parser doesn't accept `1e9`.

## What is deliberately not patched

Light-engine queues, entity and chunk-section tracking keys, and terrain noise all have their own assumptions about coordinate range. Beyond about 33.5 million these are expected to misbehave (aliasing, missing entities, repeating terrain, or a server crash). That is the experiment, not a bug. As far as I know terrain noise repeats every 33,554,432 blocks in vanilla.

## If it fails at startup

Open `logs/latest.log` and look for `InvalidInjectionException` or `Mixin apply failed`. It names the mixin class. Add that class's simple name to `disabledMixins` (for example `["PlayerEntityMixin"]`) and relaunch. The game will start, but that particular limit will still be enforced. The `readData` patch in `EntityMixin` is optional (`require = 0`) and skipped quietly if the target isn't found; the only effect is that a player who logs out beyond the old limit is moved back on rejoin.

## Credits

The idea of which vanilla checks to patch was informed by [BorderRemover](https://github.com/PercyDan54/BorderRemover) by PercyDan (MIT). This mod is written separately, targets 1.21.6 with Yarn mappings, and adds the configurable limit and diagnostics.
