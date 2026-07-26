# Dungeon Builder Studio — Product and Technical Specification

## 1. Mental model

The Studio has three reusable asset types. Keeping them separate is the most important architectural decision in the system.

1. **Room Asset** — a captured Minecraft structure, its room role, sockets, and semantic anchor points.
2. **Mob Pool** — weighted entity or entity-tag entries, including optional-mod requirements, level ranges, and XP.
3. **Dungeon Definition** — rules or fixed connections that compose room assets and mob pools into a playable dungeon.

Changing a dungeon's room weight does not change the default weight of that room in another dungeon. Updating a mob pool updates every dungeon that intentionally references it. Updating room blocks requires an explicit new snapshot and never happens merely because the creator walked away or exported.

## 2. Room authoring lifecycle

### Build

Build one self-contained room in the Dungeon Builder dimension. A procedural room should include its walls at every possible socket. The generator removes only the authored socket opening when that socket is actually connected; unused optional sockets remain solid.

### Define bounds

Use the Surveyor Wand to select the smallest complete box containing the room. Mark one room region inside it. All regions, sockets, and anchors must remain inside the structure bounds.

### Add sockets

A socket is the exact doorway opening, normally one block inside the wall, plus its outward facing direction.

- `required` means the final dungeon is invalid unless the socket is connected.
- `optional` means the planner may connect it; otherwise the wall stays intact.
- Start and boss rooms normally have one required socket.
- Normal through-rooms normally have two required sockets.
- Junctions use two required route sockets plus optional branch sockets.

Sockets connect only when type, opening dimensions, vertical/horizontal class, and opposing direction are compatible after rotation.

### Add generic anchors

The Encounter Wand initially places a generic `spawn_point`. Saving a room never requires choosing a mob. In the Studio, clicking the point assigns:

- a role: normal, elite, or boss;
- an encounter group;
- a mob pool referenced by that encounter;
- automatic generation-time spawning or optional delayed trigger behavior.

Player start, return portal, loot, feature, and boss-completion anchors retain their explicit semantic types. Anchors are schema metadata, not saved entities. Structure snapshots intentionally contain blocks and block entities but exclude free-standing mobs.

### Capture

`Capture Room` writes a versioned structure snapshot into a world-owned authoring store and records a checksum, size, capture minimum, timestamp, and metadata revision. Later block edits do not affect it. `Update Snapshot` replaces it atomically. Changing structure bounds invalidates the current snapshot.

Socket and anchor metadata may be edited after capture because positions are stored relative to the frozen capture minimum. Export requires a valid snapshot.

## 3. Dungeon modes

### Procedural: Linear

Builds one deterministic start-to-boss route. Normal rooms may rotate north, east, south, or west, so adjacent sockets create left/right turns. The planner uses bounded deterministic backtracking; an early turn is reconsidered if it blocks the boss.

### Procedural: Branching

First reserves and solves the complete start-to-boss critical path. It then spends the remaining room budget on optional sockets using junction, treasure, normal, corridor, and dead-end roles. A branch is accepted only if it does not overlap an existing room or passage and does not invalidate required sockets. Unused optional sockets remain closed.

### Fixed Layout

The author places and drags room instances on a zoomable blueprint canvas, rotates them, and explicitly joins compatible sockets. Linking performs the exact 3D socket snap; connected nodes are frozen until disconnected. Fixed mode is suitable for a hand-designed multi-room dungeon while still reusing Room Assets. Validation rejects overlaps, passage crossings, mismatched sockets, disconnected required sockets, missing start/boss routes, and unreachable playable rooms.

## 4. Planner contract

Preview and runtime generation must call the same server-side planner. Given the same data revision, dungeon ID, seed, room-count override, and origin constraints, they return the same room selection, rotations, bounds, and connections.

Planning is side-effect-free. Placement, passage carving, encounters, portals, and bedrock shell generation begin only after a plan succeeds.

Randomness uses independent seed-derived streams for layout, effective level, encounters, and rewards. Adding a random encounter roll therefore cannot change the room layout for an existing seed.

The planner returns a successful or best-partial layout plus bounded diagnostics:

- missing or stale structure snapshot;
- socket type, facing, or opening mismatch;
- room or passage collision;
- build-height violation;
- required socket left open;
- no remaining path to a boss room;
- search budget exhausted.

Collision checks use full 3D bounds. The top-view simulator includes a floor/Y filter so vertically separated rooms are not shown as false visual overlaps.

## 5. Studio workspace

Press `N` in the Dungeon Builder dimension to open the Studio directly. Normal worlds retain the existing System menu behavior.

### Rooms

Room library, active physical project, capture state, role, default weight, bounds, footprint preview, sockets, anchors, and room validation. Selecting a validation item selects and centers the affected element when possible.

### Anchors

Top-down clickable anchor list and inspector. Multi-selection can assign several points to one encounter. Unconfigured generic spawn points are TODO warnings and are omitted from compiled JSON; required room-role anchors remain blocking errors.

### Pools

Workspace-wide pool editor. Entries support exact entity IDs or `#entity_type` tags, integer weights, optional `required_mod`, eligible dungeon-level range, spawned entity-level range, and base XP. The UI displays normalized probability but stores integer weights. Missing optional mods disable an entry visibly without deleting it.

### Layout

Dungeon definition editor. Procedural mode configures topology, included rooms and per-dungeon weights, ranks, room-count range, maximum depth, and shell. Fixed mode edits instances and exact socket connections.

### Simulate

Seeded, read-only layout preview from the canonical planner. Regenerate changes the seed; editing data changes the workspace revision. Partial failed layouts remain visible with collision samples and aggregated rejection counts.

### Export

One checklist for snapshot validity, semantic validation, pool references, planner feasibility, required anchors, rank configuration, return portal, and shell. Procedural feasibility covers every configured room count at one stable seed and repeats the minimum, midpoint, and maximum at two more seeds (at most 68 plans); fixed layouts validate their exact graph once. Export stages files, validates the compiled schema, then atomically moves the completed datapack into the save's datapack directory.

## 6. Persistence and multiplayer safety

Builder metadata is server-authoritative SavedData, isolated per player workspace. Every accepted Studio edit increments a persistent revision. Client edits include the revision they were based on; stale edits are rejected and receive a fresh snapshot instead of silently overwriting newer work.

Large structure NBT is stored in a bounded world-owned snapshot directory, never inside SavedData or client packets. Packet strings, list sizes, rooms, pools, entries, and simulation diagnostics are capped before allocation. File paths are derived only from sanitized owner/namespace/name identifiers; clients never submit paths.

## 7. Exported addon contract

The Studio compiles existing schema-v2 room, dungeon, marker, encounter, and mob-pool JSON. External addons may reference exact mod entity IDs, entity-type tags, and optional `required_mod` conditions. Pool modifiers remain supported.

The exporter writes only pools that exist in the authoring catalog or intentionally generated one-entry pools for explicit entity shortcuts. It never invents or overwrites zombie example pools. Every referenced pool must have at least one active fallback entry after conditions are evaluated.

The exported datapack includes a generated README with activation, reload, test-generation, dependency, and editing instructions.
