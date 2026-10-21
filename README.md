# Just Enough Right-Clicks

A JEI addon that lists what a block becomes when you right-click it with a tool.
It reads the transformations the game and installed mods already register, so
stripped logs, waxed copper and modded wood appear without a hand-written table.

Client side only. Requires JEI for Minecraft 1.21.1 on NeoForge.

## Build

Use JDK 21:

```bash
_tools/gw <path-to-this-repo> build
```

The jar is written to `build/libs/just-enough-right-clicks-0.1.0+neoforge-1.21.1.jar`.

## Verification

The block enumeration can run on a dedicated server for inspection:

```bash
java -Djustenoughrightclicks.verify=true ... nogui
```

It logs the entry count and writes `config/justenoughrightclicks-dump.txt`.
The 2026-10-21 run is recorded in `_docs/VERIFY.md`.

## License

All Rights Reserved. See [LICENSE](LICENSE).
