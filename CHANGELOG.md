# Changelog

## 0.1.0 — 2026-10-21

- First release.
- Adds a JEI category listing tool right-click transformations: axe strip, axe scrape,
  axe wax off, shovel flatten, hoe till and honeycomb waxing.
- Every row shows the source block, every item that can perform the action, and the result.
- Source and result blocks can both be looked up with JEI's recipe and usage keys.
- Blocks from other mods are listed when those mods register the transformation through
  the vanilla or NeoForge tool-modification hooks.
- Rows that only change block state, and blocks without an item form, are omitted.
- Client side only; does not need to be installed on a server.
