# Dungeon Builder Studio: First Three-Room Dungeon

This tutorial makes a complete procedural dungeon named `test:simple_dungeon` with exactly three rooms:

- `test:simple_start` - player arrival and return portal;
- `test:simple_normal` - one automatic normal encounter;
- `test:simple_boss` - one automatic boss encounter.

It is deliberately small. It proves that capture, rotation, sockets, encounters, simulation, export, and runtime generation all work. With only one room asset for each role, it will not have much visual variety yet; add alternate normal and junction rooms after this version works.

## Before building

Create a **Dungeon Builder** world and enter Creative mode as an operator. Press **N** in this dimension to open **Dungeon Builder Studio**. In an ordinary world, N continues to open the normal Solo Leveling System menu.

The Studio keeps three different things:

1. A **Room Asset** is one physical room, its sockets, and its semantic points.
2. A **Mob Pool** is a reusable weighted list of entities or entity-type tags.
3. A **Dungeon Definition** chooses room assets, ranks, room count, and generation rules.

The critical rule is: **one room project represents one physical room**. Never put the start, normal, and boss builds inside the same room project.

Build the three rooms at least 24 blocks apart so their authoring bounds cannot overlap. An 11 x 7 x 11 sealed box is a convenient first size. Give it a solid floor, walls, and ceiling. Do not cut doorway holes yourself.

### Wand controls

- Sneak + right-click air cycles the held wand's mode.
- Right-click a block uses the selected mode.
- A two-point selection uses two opposite corners.
- The active room is shown in the Builder HUD and at the top of the Studio. Check it before every wand action.
- Use **Refresh** in the Studio after changing something with a wand.

Every wand point is selected by right-clicking a block. For an air corner (such as the upper Room Bounds corner or a socket one block behind a wall), temporarily place **Structure Void** or another guide block there, click it, then remove it before **Capture Room**. Guide blocks are only selection handles; they are not anchors.

### Bounds and sockets

For every room:

1. With the **Surveyor Wand**, select **Structure Bounds** from one outside corner of the complete room to the opposite outside corner. Include the floor, ceiling, and outer walls, but no unrelated build.
2. Select **Room Bounds** around the usable interior volume.
3. A socket is the exact doorway rectangle, normally one block behind the outer wall. For a 3 x 3 doorway, select its two opposite corners on one wall plane.
4. The face clicked for the socket's first corner determines its outward direction. The arrow in the Studio top view must point out of the room.
5. Keep the blocks in the doorway plane. The generator removes an opening only when that socket is connected.

Do not place chests, signs, or other block entities inside a socket rectangle.

## 1. Build and capture the start room

1. Build the first sealed box.
2. Press **N**, open **ROOMS**, and press **New Room**.
3. In **CREATE ROOM PROJECT**, enter the exact Room ID `test:simple_start`, leave `TYPE: MODULE`, and press **Create**.
4. Select it in **ROOM LIBRARY**. Press **Role** until the inspector says `START`.
5. Close the Studio and select this room's Structure Bounds and Room Bounds with the Surveyor Wand.
6. With the **Socket Wand**, use **Required Corridor** to mark exactly one 3 x 3 socket. Leave its wall sealed.
7. With the **Feature Wand**, place one **Player Start** on the floor where players arrive.
8. Place one **Return Portal** on a clear floor position. This is where the generated exit portal appears.
9. Press **N**, select `test:simple_start`, and press **Refresh**. Click the socket in the top view and confirm its arrow points outward and its policy is `MUST CONNECT`.
10. Press **Capture Room**. The room library should change from `TODO CAPTURE` to `CAPTURED`.

The capture is an explicit block snapshot. It contains blocks and block entities, not free-standing mobs. Player start, portal, and socket positions are saved as metadata.

## 2. Build and capture the normal corner room

1. Build the second sealed box away from the first.
2. In **ROOMS**, press **New Room**, enter `test:simple_normal`, leave `TYPE: MODULE`, and press **Create**.
3. Press **Role** until the inspector says `NORMAL`.
4. Select only this room's Structure Bounds and Room Bounds.
5. Mark exactly two **Required Corridor** sockets.

For this tutorial, put the two sockets on **adjacent walls**. This makes the room a 90-degree corner and proves that the planner rotates rooms. If they are on opposite walls, the room is a straight-through room instead. Both designs are valid.

6. Cycle the **Encounter Wand** to **Unassigned Spawn Point**. Click one or more clear floor blocks where normal mobs may appear.
7. Do **not** create a Trigger Region. Automatic spawning is the default.
8. Open the Studio, refresh, inspect both socket arrows, and press **Capture Room**.

The yellow generic spawn points are intentionally unconfigured for now. They are anchors, not saved entities. You will assign their encounter in the Studio after creating a mob pool.

## 3. Build and capture the boss room

1. Build the third sealed box.
2. Create `test:simple_boss` with `TYPE: MODULE` and cycle **Role** to `BOSS`.
3. Select this room's Structure Bounds and Room Bounds.
4. Mark exactly one **Required Corridor** socket for its entrance.
5. With the Encounter Wand, place one **Unassigned Spawn Point** on a clear floor block where the boss should appear.
6. Do not place a Trigger Region for this tutorial.
7. Refresh the Studio and press **Capture Room**.

The boss room does not need to face the direction in which you built the start room. During planning, all four horizontal rotations are tested and the boss entrance is aligned with the final open socket.

## 4. Create the normal mob pool

Open **POOLS**, press **New Pool**, enter `test:room_mobs`, and press **Create**. Press **Add Entity** for each row. In the entry dialog, the first button switches between **Entity** and **Tag**; the Eligible, Spawn Level, and XP buttons enable or disable their adjacent fields. **Suggest** cycles loaded entity IDs, but you may type an optional-mod ID manually. Enter the following values, press **Add** for each row, and then press **Save Draft**.

| Selector type | Entity or tag ID | Weight | Required mod | Eligible dungeon level | Spawned entity level | Base XP |
| --- | --- | ---: | --- | --- | --- | ---: |
| Entity | `sololeveling:d_knight_1` | 3 | leave blank | leave unset | 1-10 | 10 |
| Tag | `minecraft:skeletons` | 2 | leave blank | 1-10 | 1-10 | 8 |
| Entity | `examplemobs:crypt_guard` | 1 | `examplemobs` | 5-10 | 5-10 | 20 |

`examplemobs:crypt_guard` is an example of an entity supplied by another mod. Replace both `examplemobs` values with the real mod ID and entity registry ID you want. It is safe to keep an optional entry while that mod is absent: `required_mod` disables that row without deleting it. Always keep at least one unconditional entry so the pool cannot become empty.

For a tag row, choose **Tag** and enter the tag ID without the display `#`; the Studio shows it as `#minecraft:skeletons`.

- **Weight** is relative. With all three rows active, their chances are 3/6, 2/6, and 1/6. If `examplemobs` is absent, the first two renormalize to 3/5 and 2/5.
- **Eligible dungeon level** controls when a row may be selected.
- **Spawned entity level** controls the Solo Leveling level applied to the selected mob. Leaving it unset uses the dungeon's effective level.
- **Base XP** controls Solo Leveling XP before normal multipliers. `0` deliberately disables XP for that entry; leaving it unset uses automatic behavior.

The Studio exports exactly the entries you authored. It does not invent a zombie fallback or silently replace an empty pool.

## 5. Create the boss pool

Still in **POOLS**, create `test:boss_pool` and add this entry:

| Selector type | Entity ID | Weight | Required mod | Eligible dungeon level | Spawned entity level | Base XP |
| --- | --- | ---: | --- | --- | --- | ---: |
| Entity | `sololeveling:goblin_king` | 1 | leave blank | leave unset | 10-10 | 100 |

Press **Save Draft**. A boss pool may contain several weighted bosses, exact external-mod IDs, or entity-type tags just like a normal pool.

## 6. Assign the normal and boss anchors

Open **ANCHORS**.

### Normal room

1. Select `test:simple_normal` in the room library.
2. Click a yellow generic point in the top view. Its inspector initially shows `TODO NOT ASSIGNED`.
3. Press **Next Role** until `SPAWN ROLE` says `NORMAL` and `KIND` says `MOB_SPAWN`.
4. Press **Next Pool** until `MOB POOL` says `test:room_mobs`.
5. Press **Configure**, set Encounter ID to `room_mobs`, leave `LEVEL: INHERIT`, and press **Apply**. Inherit lets the selected pool entry's Spawn Level apply; if that entry has no Spawn Level, it uses the dungeon's effective level.
6. Leave **Delayed** off. The inspector must say `ACTIVATION: ON GENERATION`.
7. Repeat for every generic point, using the same encounter ID and pool.

All points in one encounter group activate together. Runtime rolls the weighted pool **independently for every spawn point**. If any selected mob cannot spawn, the successful spawns from that attempt are discarded and the whole encounter retries later; a wave never remains half-spawned. These mobs are not stored in the room snapshot.

### Boss room

1. Select `test:simple_boss` and click its generic point.
2. Press **Next Role** until it says `BOSS` / `BOSS_SPAWN`.
3. Press **Next Pool** until it says `test:boss_pool`.
4. Press **Configure**, set Encounter ID to `boss`, switch to `LEVEL: OVERRIDE`, enter minimum `10` and maximum `10`, and press **Apply**. An override takes precedence over a pool entry's Spawn Level.
5. Leave **Delayed** off so the boss activates automatically.

Player Start and Return Portal already have explicit meanings and do not need a mob pool.

## Automatic encounters versus Trigger Regions

No player has to walk through a hidden box for this dungeon. With **Delayed** off, encounters activate automatically as part of dungeon generation once their marker chunks are ready.

A **Trigger Region** is only for an intentionally delayed fight:

1. Use the Encounter Wand's Trigger Region mode to select a volume.
2. In **ANCHORS**, click the trigger outline, press **Configure**, enter the same Encounter ID used by its spawn points, and press **Apply**.
3. Select a spawn anchor in that encounter and verify the inspector now says `DELAYED BY TRIGGER`.

Assigning the Trigger Region enables delayed activation automatically. A trigger owns only its encounter ID; role, pool, level, XP, and activation are shared encounter configuration exposed through its spawn anchors. Turning **Delayed** off afterward makes the encounter automatic again. If Delayed is requested without a matching trigger, validation warns and the encounter remains automatic. Trigger regions, like spawn points, are JSON metadata rather than invisible entities stored in the structure.

## 7. Create the procedural dungeon definition

Open **LAYOUT**. In the saved-dungeon catalog, press **New**, enter the exact ID `test:simple_dungeon`, and create the draft. Then set:

| Setting | Value |
| --- | --- |
| Active saved draft | `test:simple_dungeon` |
| Mode | `PROCEDURAL` |
| Topology | `LINEAR` |
| Enabled room assets | `test:simple_start`, `test:simple_normal`, `test:simple_boss` |
| Per-dungeon room weights | 1 for all three |
| Minimum rooms | 3 |
| Maximum rooms | 3 |
| Maximum depth | at least 3; 8 is a safe value |
| Allowed ranks | D only |
| Shell block | `minecraft:bedrock` |
| Shell thickness | 1 |

Select each room in the left library and press **Include**. Once included, set its **DUNGEON WEIGHT** to `1`. Use **Mode**, **Topology**, **Min - / Min +**, and **Max - / Max +** for the remaining rules. Press **Setup**: press **ALL** to clear the individual rank chips, press **D** so only D is lit, set Maximum Depth to `8`, use the **Bedrock** shortcut, set thickness `1`, and press the dialog's **Apply**. Finally press the Layout tab's **Apply** to commit every layout edit before simulating.

The **DEFAULT WEIGHT** shown in ROOMS is only the starting value copied when an asset is first included. **DUNGEON WEIGHT** in LAYOUT is the actual relative probability for this saved dungeon, among eligible rooms of that role. Changing a room's default later does not silently rewrite existing dungeon drafts.

The catalog keeps multiple definitions. **Open** restores the selected saved draft; apply current edits first if you want to keep them. **Setup** edits the active draft's rank and shell settings locally; its ID was chosen by **New** and cannot silently overwrite another draft. **Delete** opens a confirmation dialog before removing a draft and does not delete its reusable room assets or mob pools. Unsaved layout edits survive while you select different room-library entries, but Preview, Validate, and Export require the Layout **Apply** button.

Do not build a bedrock box by hand. The runtime first finds a complete collision-free plan, places the rooms, carves only connected socket openings, and then applies the configured bedrock shell.

## 8. Simulate seed 12345

Open **SIMULATE**, enter `12345` in the seed field, and press **Run Preview**.

A successful preview shows three footprints and two connections. Click a generated room to inspect its source asset and rotation. This preview uses the same server-side planner as runtime generation.

What the planner is doing:

- It rotates each module north, east, south, or west to align compatible sockets.
- An adjacent socket pair creates a left or right turn after rotation; an opposite pair stays straight.
- A required socket must connect. An unused optional socket remains a sealed wall.
- Full 3D room and passage bounds are checked before placement.
- Bounded deterministic backtracking revisits earlier room and rotation choices when a later room or the boss would collide.

This definition is **linear**, so it reserves one start-to-boss route. A branching dungeon needs junction assets with optional branch sockets, compatible dead-end/treasure/normal assets, and a room budget larger than the critical path. Branches are accepted only when they do not collide or leave required sockets open.

If seed 12345 fails, do not keep changing seeds to hide the problem. Read the diagnostic, then check socket facing, matching opening sizes/types, required-socket counts, room snapshots, and build-height limits.

## 9. Validate and export

Open **EXPORT** and press **Validate**. The expected result is zero blocking errors. Warnings are shown separately. Clicking an issue selects its room when possible.

The checklist should confirm:

- all three room snapshots exist and are current;
- the start has Player Start and Return Portal;
- the boss has a configured Boss Spawn;
- every referenced pool exists and has an active fallback entry;
- the rank and bedrock shell are configured;
- the canonical planner passes its export coverage matrix. Procedural layouts test every configured room count at seed `0`, then the minimum, midpoint, and maximum at two additional stable seeds; a failure reports the exact count and seed.

Press **Export Pack** only after validation passes. Export stages and validates the pack before moving the completed folder into this save's `datapacks` directory. Use the exact folder name reported by the Studio.

Enable and reload it:

```mcfunction
/datapack enable "file/<exact-exported-folder>"
/reload
/slrdungeon issues
/slrdungeon pool test test:room_mobs 5
```

Then generate the exact preview seed:

```mcfunction
/slrdungeon generate test:simple_dungeon seed 12345 confirm
```

Use the instance UUID printed by the command:

```mcfunction
/slrdungeon enter <instance-uuid>
```

For normal gameplay, stand within eight blocks of an unused compatible D-rank procedural gate and bind it:

```mcfunction
/slrdungeon bindgate test:simple_dungeon
```

To use the dungeon in another save, copy the whole exported folder into that save's `datapacks` directory, enable it there, run `/reload`, and inspect `/slrdungeon issues` before generating it.

## What saves automatically?

- Accepted room, socket, anchor, and pool edits are server-authoritative and autosave with a workspace revision.
- Layout edits remain in the active editor while you select other rooms, but are committed to the saved dungeon only when you press **Apply**.
- Closing the Studio does not discard accepted metadata edits.
- Physical room blocks change in the asset only when you press **Capture Room** or **Update Snapshot**.
- Editing the live build after capture does not silently alter an export.
- If you intentionally change the room blocks, press **Update Snapshot**.
- If Structure Bounds change, capture a new snapshot before simulation or export.

This split is deliberate: metadata can be edited safely, while an accidental block change cannot corrupt a known-good room.

## Legacy command note

Commands remain useful for status, automation, and recovery, but they are not a substitute for this Studio tutorial. In particular, `/dungeonbuilder encounter select/configure` configures older concrete encounter markers; it does **not** assign the generic Spawn Points placed by the current Encounter Wand. Assign those points in **ANCHORS**, and use the saved-dungeon catalog plus Layout **Apply** for this dungeon.

## Common fixes

### "Outside the structure bounds"

- Confirm the correct room is active in the HUD.
- Reselect Structure Bounds around only that room.
- Reselecting Room Bounds replaces the old room region for that project.
- Use Builder Wand **Erase** near an unwanted marker, socket, or region.

### Preview cannot reach the boss

- Start and boss rooms normally need exactly one required horizontal socket.
- A normal through-room normally needs two required compatible sockets.
- Confirm socket arrows point outward and opening widths/heights match.
- Optional branch sockets are not substitutes for the two required critical-route sockets.

### `Unknown or Invalid dungeon definition test:simple_dungeon`

- Enable the exact exported folder, not its parent directory.
- Confirm the pack contains `data/test/slr/dungeons/simple_dungeon.json`.
- Run `/reload`, then `/slrdungeon issues` and inspect `latest.log`.
- Do not rename only the folder or JSON after export; IDs inside the compiled files must still agree.
