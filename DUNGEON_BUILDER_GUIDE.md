# Solo Leveling Dungeon Builder

The Dungeon Builder creates playable, addon-friendly dungeons without saving invisible marker entities. A structure NBT stores only blocks and block entities. Allowed gate ranks, sockets, marker IDs, encounter groups, mob pools, levels, boss rules, and the bedrock shell are stored as schema-v2 datapack JSON.

That separation is intentional: creators can edit or extend a dungeon with a datapack, and an optional mob from another mod never becomes a broken entity embedded inside a structure.

## Start a Builder world

1. Create a world with the **Dungeon Builder** world preset.
2. Enter in Creative mode as an operator. Operators receive all five 16x16 architect wands.
3. The wands are also in vanilla **Operator Utilities** when operator-tab items are enabled.
4. Press **N** to open **Dungeon Builder Studio**. In a Builder world, N opens the Studio instead of the normal Solo Leveling System menu.

Builder state is stored per player in world SavedData, not on the wand. Copying or replacing a wand cannot lose or mix projects.

For a first build, follow `DUNGEON_BUILDER_THREE_ROOM_TUTORIAL.md`. `/dungeonbuilder tutorial` and `/dungeonbuilder help` remain available as command-line help, but the Studio is the primary workflow. The Builder-only HUD on the right shows the active room, wand mode, bounds/counts, required checklist, and condensed validation errors.

The normal Studio sequence is:

1. **ROOMS:** create or select one room asset, build it, mark its geometry, then explicitly **Capture Room**.
2. **POOLS:** create reusable weighted mob pools.
3. **ANCHORS:** turn generic spawn points into normal, elite, or boss anchors and assign their pool and encounter ID.
4. **LAYOUT:** create/open a saved dungeon definition, include room assets, set per-dungeon weights, ranks, room count, topology, and shell, then press **Apply**.
5. **SIMULATE:** preview a seed with the real server planner.
6. **EXPORT:** validate and explicitly export the datapack.

## Controls

- Right-click a block: perform the current wand mode.
- Sneak + right-click air: cycle the held wand's mode.
- Right-click air: show status, or run a Builder Wand action.
- Sneak while clicking chests, doors, or other interactive blocks.
- Use vanilla **Structure Void** blocks as invisible guides in empty space. They are omitted from the captured NBT.
- Two-point modes remember the first corner until the second is selected. Builder Undo cancels an unfinished selection first.

## Room assets and dungeon drafts

In **ROOMS**, press **New Room** and enter an exact lowercase `namespace:name` ID. Choose:

- `MODULE` for one sealed room that a procedural or fixed layout can reuse; or
- `PRESET` for one complete prebuilt dungeon captured as a single structure.

One module project must represent one physical room. Do not place a start room, normal room, and boss room inside one module project's Structure Bounds.

In **LAYOUT**, module authors manage a separate catalog of saved dungeon definitions. **New** creates a definition with its own exact ID, **Open** restores a saved definition, and **Delete** requires confirmation. A definition stores its included room assets and its own weight for each one. The **DEFAULT WEIGHT** in ROOMS is only copied when a room is first included; **DUNGEON WEIGHT** in LAYOUT is the probability used by that dungeon.

Press the Layout tab's **Apply** button before Preview, Validate, or Export. Selecting another room asset does not discard the current layout edit buffer, but opening, deleting, or replacing a saved draft requires the current edits to be applied first.

The older project commands remain useful for scripting and recovery:

```mcfunction
/dungeonbuilder project new <namespace> <name> <preset|module>
/dungeonbuilder project list
/dungeonbuilder project select <namespace> <name>
```

Use one stable lowercase namespace for every room belonging to an addon. A `preset` is one complete fixed structure. A `module` is one sealed box that the procedural assembler may rotate and connect.

Destructive workspace operations require confirmation:

```mcfunction
/dungeonbuilder project reset confirm
/dungeonbuilder project delete <namespace> <name> confirm
```

Exports are versioned and never overwrite an older datapack.

## The five wands

### Surveyor Wand

- **Structure Bounds:** two inclusive opposite corners around exactly one captured structure.
- **Room Bounds:** a gameplay/activation volume. Mark at least the walkable room area.

Every exported coordinate is normalized relative to the minimum structure corner.

### Socket Wand

- **Corridor:** optional horizontal connection.
- **Required Corridor:** a horizontal connection that must be used or generation fails.
- **Stair Up / Stair Down:** optional matching vertical `stair` connections.
- **Required Stair Up / Down:** vertical connections that generation must use.

A socket is a doorway rectangle, not a point. Click its two opposite corners on the same plane. The first clicked block face is its outward direction.

Place the socket either:

- on the inside face of the outer wall; or
- exactly one block behind that wall, which is the recommended layout.

Do not pre-cut the doorway. Keep the room as a sealed box. When two sockets match, the runtime aligns their type, dimensions, and opposite directions, then carves only the small union between their two marked planes. Unused optional sockets remain solid.

### Encounter Wand

- **Unassigned Spawn Point:** one generic mob position. Its gameplay meaning is assigned later in the Studio.
- **Optional Delayed Trigger Region:** two-corner volume that deliberately postpones an encounter until a participant enters it.

Spawn markers are placed in the air beside the clicked face. Click the top of the floor to place one at standing height.

The wand does not decide whether a point is normal, elite, or boss, and it does not save an entity into the structure. After placing points, open **ANCHORS**, select a point in the room preview, then use:

- **Next Role** to choose `NORMAL`, `ELITE`, or `BOSS`;
- **Next Pool** to choose an authored mob pool; and
- **Configure** to set its Encounter ID and either inherit the dungeon/pool level or override it with a finite range.

Points with the same Encounter ID activate as one encounter. The pool is rolled independently for every compatible point. With **Delayed** off, the encounter activates automatically during dungeon generation when its marker chunks are ready. If one selected mob cannot spawn, that attempt is rolled back and the complete encounter retries; it is not left half-spawned.

A Trigger Region is optional. In **ANCHORS**, select its outline and use **Configure** to give it the same Encounter ID as the spawn points. Assigning the trigger enables that encounter's delayed activation; select one of its spawn points to verify that the inspector says `DELAYED BY TRIGGER`. The trigger owns only the Encounter ID; role, pool, level rule, and delayed state belong to the encounter's spawn configuration. Trying to enable **Delayed** on a spawn point without a matching trigger is refused, the encounter remains automatic, and the Studio reports the problem.

The old `/dungeonbuilder encounter ...` commands remain for legacy concrete-marker workspaces, but they do not replace assigning a new generic point in **ANCHORS**. Use a custom Studio pool such as `myaddon:crypt_mobs` when you want weighted random entities, tags, mod conditions, or custom XP values.

### Feature Wand

- **Player Start:** arrival anchor.
- **Exit:** completion/transition anchor.
- **Return Portal:** return anchor.

Feature anchors are metadata. They are not saved armor stands or display entities.
`loot` and `checkpoint` marker types remain available to addon JSON as reserved metadata, but schema v2 deliberately does not pretend they have built-in behavior yet.

### Builder Wand

- **Studio:** opens Dungeon Builder Studio. Pressing **N** is the faster equivalent.
- **Capture:** explicitly saves or updates the selected room's block snapshot.
- **Status:** active project, role, group, and counts.
- **Preview:** particles for bounds, regions, socket planes/directions, and anchors.
- **Validate:** quick actionable errors and warnings for the active room.
- **Undo:** restores up to 32 edits from the current play session.
- **Erase:** right-click within eight blocks of the nearest marker, socket, or region.
- **Export:** opens the Studio, where **Validate** and **Export Pack** compile the selected layout, saved room snapshots, anchors, and pools.

Preview colors are cyan for structure bounds, purple for regions, orange for sockets, red for encounters, and gold for features.

Room metadata saves as you edit, but blocks change only when you press **Capture Room**, **Update Snapshot**, or use Builder Wand **Capture**. Preview, Validate, and Export never recapture a room implicitly. If you rebuild a captured room, update its snapshot before simulating or exporting.

## Build one preset dungeon

1. Press **N**, open **ROOMS**, choose **New Room**, enter the preset's exact `namespace:name`, and select `PRESET`.
2. Build the entire fixed layout.
3. Select tight Structure Bounds.
4. Mark a Room Bounds volume, Player Start, Exit or Return Portal, and generic spawn points.
5. In **POOLS**, author the weighted mobs. In **ANCHORS**, assign each point a role, pool, Encounter ID, and optional level override. At least one point must be assigned `BOSS`. Add a Trigger Region only for a fight that should deliberately wait for entry.
6. In **ROOMS**, press **Preset Setup** and select allowed ranks plus the shell block/thickness. A thickness of 1 with `minecraft:bedrock` encloses the finished dungeon after placement.
7. Press **Capture Room**. Rebuilding blocks later requires **Update Snapshot**.
8. Use **SIMULATE**, then open **EXPORT**, press **Validate**, resolve every blocking error, and press **Export Pack**. Export validation checks every allowed room count at a stable seed plus additional minimum/midpoint/maximum seed cases; it is deliberately stricter than one successful preview.
9. Enable the generated pack, reload, and test it with `/slrdungeon generate <namespace:name> confirm`.

A preset exports directly and does not use the saved module-layout catalog.

## Build a procedural dungeon

Each project is one sealed, reusable room. Keep all module projects in the same namespace.

### 1. Build the start module

- In **ROOMS**, create `myaddon:crypt_start` as `MODULE` and cycle **Role** to `START`.
- Give it one outward socket for a linear dungeon. More sockets are useful only when the selected topology and room library can legally finish every required connection.
- Place one Player Start.
- Place an Exit or Return Portal where the gate's return entity should appear.
- Select bounds, inspect socket arrows in the top view, and press **Capture Room**.

### 2. Build middle modules

Create several separate projects and assign useful roles:

- `NORMAL` for ordinary rooms;
- `JUNCTION` for branch-capable rooms;
- `TREASURE` or `DEAD_END` for optional termini;
- `CORRIDOR` or `STAIR` for specialized connectors.

- A normal through-room normally has two compatible sockets.
- A junction normally has three or four.
- A treasure/dead-end room may have one, but the generator only selects one when another frontier can still reach the boss.
- Socket rectangles that should connect must have identical transverse dimensions and the same socket type.
- Adjacent socket sides produce turns after rotation; opposite sides produce straight passages. The planner tests all four horizontal rotations and rejects room/passage collisions.
- Place generic spawn points, assign them in **ANCHORS**, and explicitly capture every finished room.
- Different shapes and per-dungeon weights create variety; the seed makes a successful layout reproducible.

### 3. Build the boss module

- Create `myaddon:crypt_boss` as `MODULE` and set its role to `BOSS`.
- Give it one entrance socket for a linear dungeon.
- Place at least one generic spawn point, then assign `BOSS`, a boss pool, and an Encounter ID such as `boss` in **ANCHORS**.
- Add and match a Trigger Region only if the boss should wait for entry. Automatic activation is the default.
- A schema-v2 encounter contains exactly one wave. The dungeon completes only after every configured boss encounter has finished.
- Capture the room explicitly.

### 4. Create, simulate, and export a saved layout

1. In **LAYOUT**, press **New** and enter the exact dungeon ID, for example `myaddon:crypt`.
2. Choose `PROCEDURAL` and a `LINEAR` or `BRANCHING` topology.
3. Select room assets in the library and press **Include**. Set each included asset's **DUNGEON WEIGHT**; this affects only the active saved dungeon.
4. Set minimum/maximum rooms. Open **Setup** to set maximum depth, allowed gate ranks, and the final shell, then press the dialog's **Apply**. For one rank, press **ALL** to clear individual chips and then light only that rank.
5. Press the Layout tab's **Apply** to save the definition. The catalog can keep multiple independent dungeon definitions built from the same room library.
6. In **SIMULATE**, run several known seeds and inspect placements, rotations, connections, and failures.
7. In **EXPORT**, press **Validate**, fix blocking errors, then press **Export Pack**.

Export compiles the saved layout with the last explicit snapshot of every included room. It does not capture nearby projects, scan an entire namespace, or silently update changed blocks.

The assembler plans the full graph before changing blocks. It rotates candidates, aligns compatible sockets, rejects room overlaps and build-height violations, honors maximum depth and required sockets, places every NBT, carves connected passages, then adds the protective shell last.

## Build a fixed multi-room layout

Use `FIXED` when you want several reusable module snapshots but an exact authored arrangement instead of procedural selection. This is different from `PRESET`: a preset is one structure snapshot, while a fixed definition places and connects several module snapshots.

1. Create and capture the module rooms normally, including one `START` and one `BOSS`.
2. In **LAYOUT**, create/open a saved dungeon definition and press **Mode** until it says `FIXED`.
3. Select a captured room asset and press **Add** for every copy needed. Fixed nodes use exact placements, so dungeon weights are ignored.
4. Select and left-drag an unconnected node in the top view for quick positioning. Use `-X`, `+X`, `-Z`, `+Z`, and **Rotate** for one-block precision. Middle-drag pans and the wheel zooms; connected nodes stay fixed until disconnected so their socket geometry cannot be corrupted.
5. Click a colored socket point on the room (or use **Socket** for keyboard-style cycling), then press **Link**. Select a compatible socket on a different node and press **Link** again. The Studio snaps the unconnected node, including its Y level, and rejects occupied endpoints, incompatible socket dimensions/types/facing, collisions, and links that would break already-connected geometry. Select a connected socket and press **Unlink** before moving or rotating that branch.
6. Use **Setup**, press the Layout tab's **Apply**, then Simulate, Validate, and Export exactly as for a procedural definition.

The fixed graph and socket links are compiled into `placements` and `connections` in the dungeon JSON. The runtime still preflights all geometry before placing any blocks and applies the protective shell last.

## Schema-v2 essentials

The Builder writes these files for you, but addon authors can edit them directly. A room module uses coordinates relative to the NBT minimum corner:

```json
{
  "format_version": 2,
  "structure": "myaddon:slr_dungeons/crypt_hall",
  "role": "normal",
  "weight": 5,
  "size": [15, 8, 15],
  "regions": [
    { "id": "room_1", "type": "room", "min": [1, 1, 1], "max": [13, 6, 13] }
  ],
  "sockets": [
    { "id": "north", "type": "corridor", "facing": "north", "required": true, "min": [6, 1, 1], "max": [8, 3, 1] },
    { "id": "south", "type": "corridor", "facing": "south", "required": true, "min": [6, 1, 13], "max": [8, 3, 13] }
  ],
  "markers": [
    { "id": "mob_1", "type": "mob_spawn", "group": "crypt", "position": [5, 1, 7] },
    { "id": "mob_2", "type": "elite_spawn", "group": "crypt", "position": [9, 1, 7] }
  ],
  "encounters": [
    {
      "id": "crypt",
      "waves": [
        { "id": "crypt_wave", "marker_group": "crypt", "pool": "myaddon:crypt_mobs", "count": 2, "boss": false }
      ]
    }
  ]
}
```

`spawn_point` is an authoring-only type and must not appear in hand-written runtime JSON. The Studio converts every assigned generic point to `mob_spawn`, `elite_spawn`, or `boss_spawn`. Unassigned points produce a warning and are omitted; any role requirement they leave unsatisfied, such as a missing boss anchor, is still a blocking error. The concrete marker keys shown above match the runtime schema.

This example auto-spawns. For an advanced delayed encounter, add a `trigger_region` region and reference its ID from the encounter with `"trigger_region": "crypt_trigger"`.

The top-level procedural definition selects role pools, level behavior, room count, and the final bedrock shell:

```json
{
  "format_version": 2,
  "generation": "procedural",
  "ranks": ["A"],
  "room_pools": {
    "start": [{ "room": "myaddon:crypt_start", "weight": 1 }],
    "normal": [{ "room": "myaddon:crypt_hall", "weight": 5 }],
    "boss": [{ "room": "myaddon:crypt_boss", "weight": 1 }]
  },
  "room_count": [6, 10],
  "max_depth": 10,
  "level": { "source": "party_average", "range": [1, 1000], "variance": 2 },
  "shell": { "enabled": true, "block": "minecraft:bedrock", "thickness": 1,
    "cover_floor": true, "cover_ceiling": true }
}
```

Every wave count must be at least one and cannot exceed the number of compatible unique spawn markers in its group. `boss: true` waves use only `boss_spawn` markers; normal waves use only `mob_spawn` and `elite_spawn` markers.

### Schema-v3 ordered waves

Set `format_version` to `3` on the room that owns the encounter (or on a preset dungeon definition) to enable ordered waves and `delay_ticks`:

```json
{
  "format_version": 3,
  "structure": "myaddon:slr_dungeons/frozen_arena",
  "role": "boss",
  "size": [31, 12, 31],
  "regions": [
    { "id": "arena_trigger", "type": "trigger", "min": [1, 1, 1], "max": [29, 10, 29] }
  ],
  "markers": [
    { "id": "north", "type": "mob_spawn", "group": "arena", "position": [15, 1, 4] },
    { "id": "south", "type": "mob_spawn", "group": "arena", "position": [15, 1, 26] },
    { "id": "east", "type": "elite_spawn", "group": "arena", "position": [26, 1, 15] },
    { "id": "west", "type": "elite_spawn", "group": "arena", "position": [4, 1, 15] },
    { "id": "baruka", "type": "boss_spawn", "group": "boss", "position": [15, 1, 15] }
  ],
  "encounters": [
    {
      "id": "red_gate_gauntlet",
      "trigger_region": "arena_trigger",
      "waves": [
        { "id": "scouts", "marker_group": "arena", "pool": "myaddon:ice_scouts", "count": 4, "delay_ticks": 100 },
        { "id": "hunters", "marker_group": "arena", "pool": "myaddon:ice_hunters", "count": 4, "delay_ticks": 160 },
        { "id": "baruka", "marker_group": "boss", "pool": "myaddon:baruka", "count": 1, "delay_ticks": 200, "boss": true }
      ]
    }
  ]
}
```

The first wave's delay starts when its trigger is entered, or when all spawn-marker chunks are ready if no trigger is configured. Each later delay starts when the preceding wave is completely defeated. Triggering and deadlines are persisted, so unloading the level or restarting the server does not reorder or skip the gauntlet. A `boss: true` wave must be last in its encounter; the instance cannot complete until that terminal wave and every predecessor in its sequence are complete. Format versions 1 and 2 retain their original one-wave, zero-delay behavior.

## Enable and test an export

```mcfunction
/datapack list available
/datapack enable "file/<exact-generated-folder>"
/reload
/slrdungeon list
/slrdungeon issues
/slrdungeon generate myaddon:crypt seed 12345 confirm
```

Generation writes a test instance from a safe origin 64 blocks in front of the operator and creates a scoped return portal at its Exit. Use a clear Builder world or dedicated dungeon dimension. Enter its saved Player Start with the UUID printed by generation:

```mcfunction
/slrdungeon instances
/slrdungeon instance <instance-uuid>
/slrdungeon enter <instance-uuid>
```

`enter` must be run from the overworld so it can save a safe return position. Recovery commands are available for addon failures:

```mcfunction
/slrdungeon encounter reset <instance-uuid> <encounter-key> confirm
/slrdungeon portal <instance-uuid>
/slrdungeon prune
```

To make an existing unused procedural gate open this datapack dungeon, stand within eight blocks of it and bind it before anyone enters:

```mcfunction
/slrdungeon bindgate myaddon:crypt
```

The bind command rejects a gate whose rank is not present in the dungeon's `ranks` array. The check runs again when someone enters, so changing a datapack cannot silently route an incompatible gate. Compatible gates generate in the normal rank destination: E and D use `dungeon_dimension_d`; C, B, A, and S use their matching dimensions. A missing `rank`/`ranks` field in an older hand-written datapack means all ranks for backward compatibility.

Unbound gates continue using the existing built-in generator. A bound gate stores the generated instance UUID and uses the normal gate party/return flow. The authored Exit or Return Portal marker determines where the scoped return portal entity appears. Before first use, remove a mistaken binding with `/slrdungeon unbindgate confirm`; generated gates cannot be rebound safely.

Removing an instance discards its loaded tracked mobs and persistent runtime record, but deliberately leaves generated blocks in place; automatically deleting a broad box in a normal world would be unsafe:

```mcfunction
/slrdungeon remove <instance-uuid> confirm
```

## Mob pools and other mods

The Studio exports exactly the pool entries you authored. It never invents a zombie fallback. Add at least one unconditional loaded entity or non-empty entity-type tag to every referenced pool, then add optional mod entries with exact registry IDs, weights, eligibility ranges, and spawn levels:

```json
{
  "format_version": 2,
  "entries": [
    {
      "entity": "sololeveling:goblin",
      "weight": 20,
      "xp": 8,
      "eligible_level": [1, 20],
      "spawn_level": [8, 12]
    },
    {
      "entity": "other_mod:armored_skeleton",
      "required_mod": "other_mod",
      "weight": 8,
      "xp": 25,
      "spawn_level": [12, 18]
    },
    {
      "tag": "myaddon:crypt_mobs",
      "weight": 12
    }
  ]
}
```

`xp` is the entry's Solo Leveling **base XP** reward. The normal gamerule, difficulty, player XP, and scaled-mob multipliers still apply; use `xp: 0` to disable Solo XP for that entry. If omitted, runtime dungeon mobs use the existing automatic reward based on their assigned level and normal/elite/boss role. The configured value also overrides the built-in reward table when a Solo Leveling mob is used as a random dungeon copy.

Test selection without spawning anything; the result prints the selected entity, assigned level, and configured or automatic base XP:

```mcfunction
/slrdungeon pool test myaddon:crypt_mobs 15
```

An addon can extend another pack's pool deterministically without replacing its file:

```json
{
  "format_version": 2,
  "target": "myaddon:crypt_mobs",
  "operation": "add",
  "conditions": [
    { "type": "forge:mod_loaded", "modid": "other_mod" }
  ],
  "entries": [
    { "entity": "other_mod:crypt_guard", "weight": 10, "xp": 30 }
  ]
}
```

Place modifiers under `data/<addon>/slr/pool_modifiers/`. Add/remove modifiers are applied in resource-ID order. Optional entries may be skipped with `required_mod` or Forge conditions, but every referenced pool must retain at least one active fallback entry. An empty live entity tag pauses and retries its encounter instead of crashing; use the encounter reset command if an addon removes a spawned mob unexpectedly.

Dungeon mobs are created through the entity registry, tagged with instance/encounter/marker IDs and optional base XP before joining the world, finalized as `MOB_SUMMONED`, assigned both canonical `slr_dungeon_level` and legacy `Level`, visibly labeled, scaled idempotently, made persistent, and tracked by UUID. The same path works for Solo Leveling mobs and ordinary `Mob` entities from other loaded mods without a Java dependency. Runtime addon mobs participate in Solo XP and level-difference combat, while story-specific boss rewards are isolated from random dungeon copies.

## Export layout and limits

```text
<world>/datapacks/<generated-folder>/
  pack.mcmeta
  README.md
  data/<namespace>/structures/slr_dungeons/<room>.nbt
  data/<namespace>/slr/rooms/<room>.json
  data/<namespace>/slr/dungeons/<dungeon>.json
  data/<namespace>/slr/mob_pools/<pool>.json
  data/<addon>/slr/pool_modifiers/<modifier>.json
```

- One captured structure is limited to 48 blocks per axis and all selected chunks must be loaded.
- A Studio procedural export is limited to 2,000,000 captured blocks across its included module library.
- Structure capture includes blocks and block entities, but excludes free entities such as mobs, armor stands, paintings, item frames, and display entities.
- Runtime planning caps a layout at 500,000 template blocks and the final shell at 250,000 changed blocks.
- Shell thickness is 0-4. Shelling fills only outside surfaces, skips every room and connected-passage interior, and uses bedrock by default.
- Shelling and passage carving are preflighted and refuse block entities so a mistaken socket cannot silently erase container NBT.
- Instance state persists rooms, connections, participants, starts/exits, encounter markers, tracked mobs, levels, seed, and completion across restarts.

Schema v2 intentionally rejects `delay_ticks` and sequential multi-wave encounters; schema v3 enables those two features. `lock_sockets`, non-default origins, room shell overrides, CAP pools, and custom `carve_depth` values remain reserved for later versions. Unused sockets remain sealed by the authored wall and the final bedrock shell.

Schema versions 1-3 are loaded atomically from `slr/rooms`, `slr/dungeons`, `slr/mob_pools`, and `slr/pool_modifiers`. Invalid resources are reported by `/slrdungeon issues` and omitted individually; a bad addon file cannot leave the runtime observing half of a reload.
