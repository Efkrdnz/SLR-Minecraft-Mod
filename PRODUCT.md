# Product

## Register

product

## Users

Minecraft mod and datapack creators working inside the Solo Leveling Dungeon Builder dimension. They build rooms in-world, mark semantic points, assemble reusable dungeon definitions, test seeded layouts, and export distributable datapacks. The interface must support both first-time creators and experienced addon authors without requiring either group to memorize commands or JSON schemas.

## Product Purpose

Dungeon Builder Studio turns physical Minecraft builds into reliable, reusable dungeon content. It separates room assets from dungeon definitions, lets creators configure sockets, encounters, mob pools, ranks, rewards, portals, and generation rules visually, and uses the same deterministic planner for preview and runtime generation. Success means a creator can understand what is missing, correct it in context, simulate multiple seeds, and export a datapack that passes reload validation without leaving the builder workflow.

## Brand Personality

Practical, precise, and trustworthy. The Studio should feel like an operator's blueprint console inside the existing Solo Leveling System: focused enough for technical work, but legible and guided rather than intimidating.

## Anti-references

- Command-heavy workflows that hide state across chat history.
- Decorative game menus that sacrifice usable space or text clarity for atmosphere.
- Unexplained icons, color-only status, overlapping labels, and clipped configuration values.
- Simulations that use different rules from real dungeon generation.
- Destructive or implicit saving where live block edits silently change an already captured room.

## Design Principles

1. Show the whole authoring state: current room, selected dungeon, unresolved work, and next valid action must remain visible.
2. Make invalid states actionable: every error identifies the affected room or anchor and provides a direct route to fix it.
3. Keep one source of truth: preview, validation, export, and runtime generation consume the same canonical data and planner.
4. Separate assets from compositions: captured rooms and mob pools are reusable resources; dungeon definitions compose them without duplicating them.
5. Preserve creator intent: metadata autosaves with revisions, while block snapshots change only through an explicit capture or update action.

## Accessibility & Inclusion

The Studio must remain usable at Minecraft's supported GUI scales and smaller window sizes. Text never overlaps or relies on hover alone; long values wrap, truncate with a tooltip, or scroll. Status always combines color with labels or symbols, focus and keyboard navigation remain visible, hit targets are at least 16 logical pixels high, and essential feedback does not rely on animation or color perception.
