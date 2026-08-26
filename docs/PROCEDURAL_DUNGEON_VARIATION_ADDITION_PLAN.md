# Procedural Dungeon Variation Addition Plan

## Goal

Add meaningful variation to built-in procedural gates through different dungeon
silhouettes, room volumes, interiors, corridors, landmarks, and boss arenas. This
is an extension of the current generator, not a replacement: the existing layout
stays available as `CLASSIC_BRANCHING` and becomes the guaranteed fallback if a
new plan cannot be validated.

The change should make two gates with the same block palette feel structurally
different while preserving reliable navigation, bounded generation cost, current
encounter balance, return-portal behavior, and compatibility with existing gates.

## Current baseline

The built-in path is:

`GateSpawnerUtil.spawnProceduralGate` ->
`ProceduralDungeonGateHandler.settingsFor` ->
`ProceduralDungeonGenerator.generate`

The current generator already has four room footprints, palette variation, floor
patterns, ceiling ribs, pillars, props, branches, junctions, and occasional
dogleg corridors. Repetition mainly comes from every dungeon using a similar
spine-with-side-branches graph, one global floor/ceiling elevation, broad L-shaped
corridors, and decoration placed over otherwise open rooms.

The advanced datapack runtime under `dungeon/runtime/layout` is intentionally not
the migration target for this addition. Moving built-in gates onto its authored
room/socket lifecycle would be a generator and save-lifecycle rework. The new
planner can borrow its deterministic planning and validation ideas without
changing the datapack schema.

## Design principles

- Plan and validate the complete dungeon in memory before changing blocks.
- Preserve `CLASSIC_BRANCHING` as 20-30% of normal rolls and as the safe fallback.
- Make topology, geometry, interiors, decoration, boss choice, and encounters use
  separate deterministic random streams.
- Keep early-rank layouts readable and introduce stronger verticality gradually.
- Change traversal and visual rhythm without letting a random layout double the
  encounter budget.
- Reserve real door-to-door navigation lanes before placing decorations or
  hazards.
- Avoid mandatory carpets, vines, deep water, soul sand, powder snow, and narrow
  holes that reproduce known follower and shadow pathfinding failures.
- Do not force-load chunks, perform neighbor-heavy decoration, or leave a partly
  built layout behind when planning fails.

## Variation layers

### Overall silhouettes

| Archetype | Shape and play rhythm |
| --- | --- |
| `CLASSIC_BRANCHING` | Existing main spine and short side branches. |
| `HUB_AND_SPOKES` | A landmark hub connects combat wings, treasure dead ends, and a distant boss wing. |
| `LOOPED_RUINS` | A main ring provides alternate approaches, with one or two shortcuts and a boss annex. |
| `FORK_AND_REJOIN` | Two meaningful routes split after entry and reunite before the boss. |
| `TERRACED_DESCENT` | Switchback rooms descend through tiers, ending in a lower boss arena. |

Rank availability:

| Rank | Available silhouettes |
| --- | --- |
| E | Classic, compact hub, and a soft loop. |
| D | E pool plus fork-and-rejoin. |
| C | All flat silhouettes; only shallow elevation changes. |
| B-S | Full pool, including terraced descent. |

No non-classic silhouette should exceed roughly 30% of its eligible pool. The
planner should also apply a bounded room-count jitter so the same rank and
complexity do not always produce the same room count.

### Room footprints

Retain `RECTANGLE`, `CHAMFERED`, `ROUND`, and `CROSS`, then add:

- `L_SHAPE`: two joined wings with an inside corner landmark.
- `T_SHAPE`: a processional stem opening into a broad encounter head.
- `OCTAGON`: a strong arena or shrine silhouette.
- `DOUBLE_LOBE`: two combat pockets connected through one shared room boundary.

Footprints only describe occupancy. Interior, height, navigation, and landmark
state must live in a sidecar plan rather than making `DungeonRoom` responsible
for all new behavior.

### Interior modules

- `OPEN`: clear combat floor with perimeter detail.
- `COLONNADE`: pillars on the edges of reserved lanes, never the center path.
- `RING_GALLERY`: perimeter route around a clear or lowered center.
- `DIVIDED_HALL`: partial walls create two connected combat lanes.
- `RAISED_DAIS`: broad stair-accessible centerpiece and open outer floor.
- `TERRACED`: two or three one-block steps with multiple ramps/stairs.
- `SHALLOW_BASIN`: a dry or one-block-deep visual basin outside critical paths.
- `BRIDGE_CHAMBER`: protected wide bridge plus safe side platforms.
- `RUINED_CHAMBER`: bounded rubble lanes and broken architecture with full
  navigation clearance.

Only one primary interior module is selected per room. Major set pieces are
budgeted separately so overlapping decorators cannot fill the room with clutter.

### Corridor modules

- Direct L corridor.
- Three-segment dogleg.
- S-bend.
- Wide gallery.
- Repeating arched hall.
- Stair gallery for elevation changes.
- Protected bridge with solid edges or rails.

The current grid-carving path remains underneath these modules. Corridor style
changes shell shape and detailing after a valid doorway-to-doorway centerline has
been found, so decoration can never break connectivity.

### Landmarks

Use at most one signature landmark per dungeon and zero to two additional major
set pieces depending on rank and complexity:

- Altar or raised throne.
- Monolith or obelisk.
- Crystal garden.
- Forge machinery.
- Tomb rows.
- Ruined fountain.
- Root shrine.
- Ritual ring.

Landmarks reserve their footprint during planning and must respect portal,
spawn, boss-clearance, and navigation masks.

## Theme architecture, not only palette

`DungeonTheme` remains the block palette. A separate variation policy maps it to
architectural shapes and landmarks:

- Stone: vaulted crypts, keeps, burial niches, and ruined fortifications.
- Deepslate: buttressed citadels, heavy arches, and stepped strongholds.
- Ice: crystal galleries, ribbed frozen caverns, and broad throne halls.
- Nether: forge platforms, basalt machinery, and contained magma ducts.
- Desert: processional halls, tomb rows, sunken altars, and stepped temples.
- Mossy: ruined courts, root shrines, and collapsed galleries with clear lanes.
- Void: obelisks, raised sanctums, and visual rifts without mandatory lethal gaps.
- Prismarine: dry aqueducts, terraced cisterns, and perimeter pools that never
  cross AI routes.

Themes may adjust weights, but must not make an interior mandatory when it is
invalid for the selected footprint, rank, or boss.

## Planning model

Add a focused package under `net.solocraft.dungeon.procedural`:

```text
ProceduralDungeonPlan
ProceduralDungeonPlanner
ProceduralLayoutArchetype
ProceduralRoomPlan
ProceduralCorridorPlan
ProceduralInteriorModule
ProceduralCorridorStyle
ProceduralNavigationMask
ProceduralVariationPolicy
BossArenaProfile
```

Suggested core records:

```java
record ProceduralDungeonPlan(
    long seed,
    int schemaVersion,
    ProceduralLayoutArchetype archetype,
    List<ProceduralRoomPlan> rooms,
    List<ProceduralCorridorPlan> corridors,
    GridBounds bounds,
    int estimatedBlockWrites
) {}

record ProceduralRoomPlan(
    int id,
    DungeonRoom footprint,
    int floorOffset,
    int interiorHeight,
    ProceduralInteriorModule interior,
    Landmark landmark,
    long localSeed
) {}

record ProceduralCorridorPlan(
    int fromRoom,
    int toRoom,
    List<GridPoint> centerline,
    int width,
    ProceduralCorridorStyle style
) {}
```

Use a 2.5D grid instead of a full 3D voxel allocation. Each occupied X/Z cell
stores:

- Existing floor/wall/corridor type.
- Floor offset.
- Ceiling height.
- Owning room ID.
- Reserved navigation flag.
- Portal/spawn/boss clearance flags.
- Hazard-permission flag.

This supports stairs, terraces, different ceiling volumes, and bridge chambers
without the memory and collision complexity of freely stacked rooms. X/Z-overlap
between rooms remains forbidden in the first version.

## Deterministic gate metadata

Persist these values on new gates:

- `slr_procedural_seed`
- `slr_procedural_layout`
- `slr_procedural_layout_version`

Derive a default seed from the gate UUID through a stable mixing function.
`ensureProceduralMetadata` fills missing values only for an ungenerated gate.
Never change the seed, layout, or stored start coordinates of a generated gate.

Create independent seed streams for:

- Topology.
- Room geometry.
- Corridor routing.
- Interior modules.
- Decoration and landmarks.
- Boss choice.
- Encounter composition.

This means adding a decorative roll cannot silently change which boss spawned or
make a bug report's seed produce a different graph.

## Source-level integration points

### `GateSpawnerUtil.spawnProceduralGate`

- Store seed, layout archetype, and schema version with the existing procedural
  gate metadata.
- Choose a rank-valid archetype with bounded weights.

### `ProceduralDungeonGateHandler`

- Extend `ensureProceduralMetadata` to derive missing planning tags safely.
- Extend `settingsFor` to pass seed, version, and archetype.
- Preserve old generated gates and all current safe-entry/return-anchor handling.

### `ProceduralDungeonSettings`

- Add seed and archetype fields.
- Preserve the existing constructor for commands, tests, and old call sites.
- Keep rank, theme, complexity, and target-room semantics compatible.

### `ProceduralDungeonGenerator`

1. Make `generate` a facade that plans, validates, and only then mutates blocks.
2. Rename the current layout method to `placeClassicLayout`.
3. Dispatch to one bounded planner per archetype.
4. Select the boss on its independent random stream before spatial planning.
5. Request the boss's arena profile before sizing the boss room.
6. Replace center-to-center corridors with planned doorway polylines.
7. Rasterize the approved plan into the 2.5D cell grid.
8. Make shell construction use per-cell floor and ceiling values.
9. Filter interior and landmark placement through navigation/clearance masks.
10. Make mob spawn, entry, portal, and return-anchor positions query the planned
    floor elevation.
11. If planning exhausts its retry budget, build deterministic
    `CLASSIC_BRANCHING` without partial mutations.

### `DungeonRoom` and `DungeonTheme`

- Add only the new footprint formulas and keep existing constructors and
  `withType` behavior compatible.
- Keep `DungeonTheme` as palette data. Put architectural behavior and weights in
  `ProceduralVariationPolicy` so the enum does not become a brittle generator.

## Navigation validation

Before world mutation, require all of the following:

- Exactly one entry room and one boss room.
- Every room is reachable from entry.
- Boss room is terminal and meets a minimum graph depth for the rank.
- Treasure rooms occupy optional branches or dead ends, not the mandatory route.
- At least 60% of eligible non-entry rooms retain combat encounters.
- Each connection reserves a minimum four-block-wide door-to-center route.
- Portal and spawn locations retain four blocks of vertical clearance.
- Decorations and hazards do not enter reserved routes or clearance discs.
- A floor transition never requires a jump over one block; larger changes receive
  continuous stairs or a bounded switchback.
- Every bridge has a safe width and protected edges.
- Critical paths never cross lava, powder snow, deep water, or open void.
- Footprint voids and terrain holes receive sealed backing.

Follower-sensitive routes should also be checked with a larger clearance sweep,
not only a single player-sized flood fill.

## Boss-aware arenas

`BossArenaProfile` supplies minimum width, length, height, central clear radius,
landing-area count, and permissions for full-height pillars, pits, water, and
ceiling clutter.

Suggested profiles:

- `STANDARD_GROUND`: broad lanes and limited edge pillars.
- `LARGE_GROUND`: reinforced perimeter, clear central melee volume.
- `AGILE_HUMANOID`: cover on edges, uninterrupted dash routes.
- `FLYING_LARGE`: tall open center, multiple landing zones, no hanging clutter.

Kaiselin uses `FLYING_LARGE`:

- Approximately 31x31 minimum floor area.
- Around 13 blocks of clear height for A-rank rolls.
- Around 16 blocks of clear height for S-rank rolls.
- Clear central flight volume and multiple broad landing zones.
- No full-height central pillar grid or roof-hanging collision clutter.

This makes the newly added A/S Kaiselin boss roll compatible with the arena
instead of fitting a flying boss into a room designed for a ground golem.

## Generation and performance budgets

- Retain the current 24-room hard cap.
- Allow at most eight complete planning attempts before classic fallback.
- Allow at most 48 placement attempts per room.
- Allow at most 12 route attempts per connection.
- Allow at most three optional loop edges.
- Clamp floor offsets to roughly -6 through +6.
- Limit the plan to about 60,000 occupied 2D cells.
- Limit estimated block writes to about 500,000.
- Limit touched chunks to about 196.
- Permit hazards on at most 6% of walkable cells and never on reserved paths.
- Keep silhouettes within approximately +/-15% of the normal encounter budget
  for the same rank and complexity.

World mutation should use the current no-drop, bounded placement approach and
avoid forced chunks or block updates that cascade into neighboring structures.

## Implementation phases

### Phase 1: deterministic foundation

- Persist seed, layout, and version metadata.
- Introduce the pure in-memory plan and independent random streams.
- Express the current generator as `CLASSIC_BRANCHING`.
- Add validation, failure diagnostics, and classic fallback.

This phase should deliberately produce familiar layouts while proving save
compatibility and reproducibility.

### Phase 2: flat silhouettes

- Add compact hub, looped ruins, and fork-and-rejoin.
- Keep one elevation while validating graphs, routing, and encounter budgets.
- Expose seed/archetype in a developer diagnostic command or log-on-demand path.

### Phase 3: interiors and architecture

- Add navigation masks, new footprints, corridor styles, interior modules, and
  landmark budgets.
- Add theme-specific architecture policies.
- Keep hazards visual-only until path validation is proven.

### Phase 4: bounded verticality

- Add per-cell floor/ceiling data, stairs, shallow terraces, and protected
  bridges.
- Enable `TERRACED_DESCENT` for high ranks after follower traversal testing.
- Keep stacked rooms out of scope.

### Phase 5: boss arenas and safe hazards

- Select bosses before planning and enforce `BossArenaProfile` constraints.
- Enable rank/theme hazard budgets only outside critical lanes.
- Add special arena policies for flying, large, and high-mobility bosses.

### Phase 6: distribution and content tuning

- Generate fixed-seed galleries for every rank/theme/archetype combination.
- Tune layout, interior, and landmark weights from real distributions.
- Compare completion time, mob count, path failures, and block-write cost.

## Regression strategy

Add planner-level tests that construct plans without a Minecraft world:

- Same seed and settings produce the same plan signature.
- A fixed seed sample reaches every rank-permitted archetype without one style
  dominating.
- Plans are connected, non-overlapping, bounded, and contain one entry/boss.
- Boss depth and treasure branch rules hold.
- Door-to-door flood fill reaches all spawn points and the boss point.
- Decorations and hazards never intersect navigation or clearance masks.
- All floor transitions are walkable.
- A/S Kaiselin plans always satisfy `FLYING_LARGE`.
- Every plan respects cell, block-write, chunk, retry, and encounter caps.
- Old settings constructors remain functional.
- Missing metadata on ungenerated legacy gates is derived safely.
- Generated gates are never rewritten.
- A deliberately invalid enhanced plan falls back to classic without mutating
  the world first.

In-game verification should then cover one fixed seed per archetype plus a sample
of random seeds with a player, a ground shadow, a flying shadow, and Kaiselin.

## Definition of done

The addition is complete when gates of the same theme visibly produce different
overall routes and interior volumes; every generated route remains traversable by
players and followers; boss arenas fit their selected boss; generation stays
within hard budgets; layouts can be reproduced from a seed; existing generated
gates are unaffected; and any invalid enhanced plan safely falls back to the
current classic generator.
