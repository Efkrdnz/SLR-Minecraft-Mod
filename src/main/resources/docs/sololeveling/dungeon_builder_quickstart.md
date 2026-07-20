# Solo Leveling Dungeon Builder Studio - Quick Start

Create a **Dungeon Builder** world and enter Creative mode as an operator. The five architect wands are given on login and are also available in vanilla **Operator Utilities**.

Press **N** in the Builder dimension to open **Dungeon Builder Studio**. Its tabs are:

- **ROOMS** - room library, roles, sockets, top view, and explicit structure snapshots;
- **ANCHORS** - assign generic points to normal, elite, or boss encounters;
- **POOLS** - weighted exact entities or entity-type tags, optional mods, levels, and XP;
- **LAYOUT** - saved-dungeon catalog, procedural/fixed mode, room selection, ranks, count, and shell;
- **SIMULATE** - deterministic seeded preview from the runtime planner;
- **EXPORT** - actionable validation and atomic datapack export.

The Builder HUD and Studio header always show the active room. **One module room equals one physical build.**

## Author one room

1. Build a sealed room. Do not cut its doorway openings.
2. In **ROOMS**, press **New Room**, enter an exact `namespace:name`, leave `TYPE: MODULE` for a procedural room, and press **Create**.
3. Press **Role** until the inspector shows the intended role.
4. Surveyor Wand: select tight **Structure Bounds**, then the usable **Room Bounds**.
5. Socket Wand: select each doorway rectangle on its wall plane, normally one block behind the outer wall. The face clicked for the first corner sets the outward direction.
6. Feature Wand: place Player Start and Return Portal for a start room.
7. Encounter Wand: place generic **Unassigned Spawn Point** anchors for combat rooms.
8. Press N, **Refresh**, inspect the top view, and press **Capture Room**.

Wands select positions by right-clicking blocks. If a required corner is in air, temporarily place **Structure Void** or another guide block at that corner, select it, then remove it before capture. This is especially useful for upper bounds and socket planes one block behind a wall.

Start and boss rooms normally have one required socket. A normal critical-route room normally has two. Opposite sockets make a straight room; adjacent sockets make a turn. Required sockets must connect. Unused optional sockets remain sealed.

Metadata autosaves. Room blocks do not: after deliberately changing a captured build, press **Update Snapshot**. Exports use the explicit snapshot rather than silently recapturing the live room. ROOMS **DEFAULT WEIGHT** is copied only when an asset is first included in a dungeon; each Layout has its own **DUNGEON WEIGHT**.

## Small start-normal-boss dungeon

Create and capture these three module assets:

| Room | Role | Required sockets | Other anchors |
| --- | --- | ---: | --- |
| `test:simple_start` | START | 1 | Player Start, Return Portal |
| `test:simple_normal` | NORMAL | 2 | one or more generic spawn points |
| `test:simple_boss` | BOSS | 1 | one generic spawn point |

For a visible turn, place the normal room's two sockets on adjacent walls. The planner tries all four horizontal rotations and rotates the boss to meet the final connection.

The full beginner walkthrough is in `DUNGEON_BUILDER_THREE_ROOM_TUTORIAL.md` at the project root.

## Mob pools and XP

In **POOLS**, create a reusable pool such as `test:room_mobs`. **Add Entity** opens the exact entry editor; its selector button switches Entity/Tag, and its Eligible, Spawn Level, and XP buttons enable their fields. Each entry can use:

- an exact entity ID, for example `sololeveling:d_knight_1`;
- an entity-type tag, for example `#minecraft:skeletons`;
- an integer weight;
- optional `required_mod`, such as `examplemobs` for `examplemobs:crypt_guard`;
- an eligible dungeon-level range;
- a spawned entity-level range;
- explicit Solo Leveling base XP.

Weights are relative and renormalize over currently eligible entries. An absent `required_mod` disables only that optional row. Keep at least one unconditional entry. XP `0` deliberately disables XP for an entry; leaving XP unset uses automatic behavior.

Across the unconditional, currently resolvable entries, eligible-level ranges must cover every possible dungeon level (1-1000). The easiest safe fallback is one unconditional entry with **Eligible** left unset; narrower rows can add level-specific variety around it.

Create a separate boss pool when desired. In **ANCHORS**, select a generic point, use **Next Role** to choose NORMAL/ELITE/BOSS, and use **Next Pool** to assign the pool. **Configure** edits its Encounter ID and chooses `LEVEL: INHERIT` or `LEVEL: OVERRIDE` with Min/Max. Inherit uses the pool entry's Spawn Level, falling back to dungeon level; override takes precedence. Generic points left unassigned are warnings and are omitted from exported JSON.

Runtime rolls the weighted pool independently for every configured spawn point. If any selected mob cannot spawn, successful mobs from that attempt are discarded and the whole encounter retries later, preventing a partial wave. Studio export writes exactly the authored pools; it does not invent or overwrite a zombie fallback.

## Automatic encounters and optional triggers

Encounters spawn automatically with dungeon activation once their marker chunks are ready. No player needs to enter a trigger area.

A Trigger Region is optional and intentionally changes an encounter to delayed behavior. Mark its volume with the Encounter Wand, click the trigger in **ANCHORS**, press **Configure**, and give it the same Encounter ID as its spawn anchors. This enables delayed activation automatically; select a spawn anchor to verify `DELAYED BY TRIGGER`, or turn Delayed off there to return the encounter to automatic activation. Triggers own only the encounter ID; pool, role, level, XP, and activation are shared encounter configuration exposed through spawn anchors. If no matching trigger exists, the encounter remains automatic and validation explains the problem.

Sockets, spawn points, triggers, portals, and encounter rules are schema metadata, not invisible entities saved in structure NBT.

## Procedural layout

In **LAYOUT**:

1. In the saved-dungeon catalog, press **New**, enter an exact ID such as `test:simple_dungeon`, and create the draft. **Open** restores another saved draft; **Delete** requires **Confirm Delete**.
2. Choose `PROCEDURAL` and `LINEAR`.
3. Select each captured start, normal, and boss asset and press **Include**.
4. Set each included room's **DUNGEON WEIGHT**. This is its actual relative chance within this dungeon; ROOMS **DEFAULT WEIGHT** was only the initial copied value.
5. Set Minimum Rooms and Maximum Rooms to `3` for the first test.
6. Press **Setup** to edit the active draft. Press **ALL** to clear the individual rank chips, press **D** so only D is lit, set Maximum Depth to `8`, use **Bedrock**, set thickness `1`, and press the dialog's **Apply**.
7. Press the Layout tab's **Apply** to commit the active draft.

Unsaved Layout edits remain while you select different room-library entries, but **SIMULATE**, **Validate**, and **Export** require Layout **Apply**. Opening another saved draft restores its committed state, so apply first if you want to keep the current edits. Deleting a draft does not delete its room assets or pools.

Linear planning reserves a complete start-to-boss route. Branching first solves that route, then spends extra room budget through optional junction sockets. Every candidate is rotated, checked in full 3D, and rejected on room or passage collision. Bounded deterministic backtracking reconsiders earlier choices if a turn boxes in the boss.

The bedrock shell is applied only after a complete plan succeeds and connected passage openings are carved.

## Simulate, validate, and export

In **SIMULATE**, enter seed `12345` and press **Run Preview**. The preview and runtime generation share the canonical server-side planner.

In **EXPORT**, press **Validate**, resolve blocking issues, then press **Export Pack**. Validation plans every allowed procedural room count and additional representative seeds, so read the exact count and seed shown by any layout-coverage error instead of hunting for a lucky preview. Use the exact folder reported by the Studio:

```mcfunction
/datapack enable "file/<exact-exported-folder>"
/reload
/slrdungeon issues
/slrdungeon pool test test:room_mobs 5
/slrdungeon generate test:simple_dungeon seed 12345 confirm
/slrdungeon enter <printed-instance-uuid>
```

To route a gameplay gate through it, stand within eight blocks of an unused compatible gate and run:

```mcfunction
/slrdungeon bindgate test:simple_dungeon
```

Binding is allowed only when the gate's rank appears in the dungeon definition. E/D use the D dimension; C, B, A, and S use their matching rank dimensions.

## Legacy command note

Commands remain available for status, automation, and recovery, but they do not replace the current Studio workflow. `/dungeonbuilder encounter select/configure` works with older concrete encounter markers and does not assign the generic Spawn Points created by the current Encounter Wand. Assign generic points in **ANCHORS**; use Studio pools for tags, weighted choices, optional mods, XP, and level ranges.

Useful diagnostics:

```mcfunction
/dungeonbuilder status
/dungeonbuilder preview
/slrdungeon list
/slrdungeon issues
/slrdungeon instances
/slrdungeon pool test <namespace:pool> <level>
```
