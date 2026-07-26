# Demon King's Castle Remake Architecture

## Player flow

1. The reusable Demon King's Castle key starts or resumes a run from the Overworld.
2. Floor 1 is generated lazily in the player's private sector of the shared DKC realm.
3. Completing a floor awards or exposes an Entry Permit but does not unlock the next floor by itself.
4. The player presents that permit to the crimson pedestal belonging to the current floor.
5. The server persists the transition claim before consuming the permit, builds the next floor in stages, then opens the physical ascension path.
6. Walking into the tower or ascension sigil moves the player to the next spatial floor cell.
7. Floor 20 completes only after both Baran and Kaiselin are defeated. Its return shrine then becomes an active exit.

## Shared realm and spatial instances

- All 20 floors use one registered dimension: `sololeveling:dungeon_dimension_dkc`.
- Runtime dimension creation/removal is deliberately avoided. Forge does not expose a safe supported lifecycle for dynamically adding and removing `ServerLevel` instances after registry synchronization, while ordinary chunks already unload when no player or ticket keeps them active.
- `DkcSpatialLayout` allocates one persistent sector per player. Each sector contains a 5 x 4 grid of 2,048-block cells, one for every floor.
- Sectors use a 16,384-block stride in a 64 x 64 allocation grid, supporting 4,096 collision-free player runs. Current coordinates remain safely inside Minecraft's world border.
- Floor identity and run ownership are derived from cell coordinates plus the player's persisted slot. Progression code never trusts a dimension ID alone.
- The shared realm uses a shallow, flat substrate with lakes, caves, carvers, structures, features, oceans, and natural spawning disabled.
- No DKC code installs permanent chunk tickets. Only occupied or actively building cells need loaded chunks; generated inactive cells persist on disk but unload through Minecraft's normal chunk lifecycle.
- The nineteen retired floor IDs remain recognized only by recovery code so players saved in an older DKC floor can be moved safely into the shared layout. Compatibility-only dimension-type stubs and biomes remain available to decode old `level.dat` references, but no retired DKC LevelStem is registered for a new world.

This makes every floor feel like a separate open world without paying for twenty registered server levels. The shared `ServerLevel` object remains registered, but the expensive parts—ticking chunks, entities, block entities, and terrain—are absent when the cells are unused.

## Durable progression

`DkcRunSavedData` is stored in the Overworld and keeps a persistent spatial slot plus three independent bit sets per player:

- `unlockedFloors`: server-authoritative floor access.
- `generatedFloors`: layouts that finished placing successfully in the current spatial-layout version.
- `armedTransitions`: Entry Permits already accepted by a pedestal.

Separating these states is intentional. A crash after permit consumption can resume the claimed build for free, while an unfinished build never opens an inert gate. When the spatial-layout version changes, generated bits are invalidated so structures are rebuilt safely; unlocked and armed progression is preserved. Existing `dkc_started`, `dkc_cleared`, and DKC coordinates remain mirrored for compatibility with older UI and procedures.

### Hold-Tab progression snapshot

- The existing quest-info key requests a compact server-authoritative DKC snapshot instead of copying server-only wave and transition fields into the large player capability.
- The snapshot resolves the owned current floor, floor name, overall cleared count, normal wave progress, Cerberus/Vulcan phases, independent Baran/Kaiselin status, House Radiru's surrender and route outcome, and the permit/build/open-tower or final-return step.
- A response is sent immediately on a valid press. While Tab remains held, the already-running DKC player tick samples at 2 Hz and transmits only when the immutable snapshot changes; there are no entity scans or always-on synchronization.
- Hold state and equality caches are transient and cleaned on release, login, logout, respawn, and server stop. The C2S edge is direction-bound, type-validated, and rate-limited, while the client rejects a cached snapshot whenever its dimension or coordinate-derived floor no longer matches.
- Inside DKC this tracker replaces the ordinary story slot, but an active Urgent Quest remains visible as a compact footer so the two concurrent objectives do not hide one another.

## Floor construction

- `DkcFloorBuilder` owns cached immutable layouts, staged structure placement, transition blocks, tower aura creation, boss ownership, safe spawns, containment, and teleports.
- Large structure templates are clipped into 16 x 16 horizontal slices and only one slice is placed per server tick. Placement uses client updates plus known-shape handling without a full neighbor-update storm, and completion actions are typed and deduplicated so interaction spam cannot amplify finalization work.
- Structure NBT is sparse: only authored blocks and deliberate AIR cuts are stored. This avoids placing hundreds of thousands of meaningless AIR records.
- Floor 1 builds the complete 240-block tower, arrival plaza, Cerberus court, lobby, and internal ascension chamber.
- Later floors compose reusable streets, markets, magma works, cathedrals, villages, ash wastes, dragon courts, forge arenas, and throne modules. Each ascent ends inside one 64 x 64 tower segment: base on floors 2-5, mid tiers on floors 6-18, and crown on floors 19-20.
- Reusing one tower segment per floor instead of cloning the complete 240-block tower adds only 18 sliced placement ticks to a normal later-floor build. The Entry Permit pedestal sits outside its closed gate, and replacing that gate with the open template makes walking into the existing chamber the transition.
- Floor 15 is the bespoke House Radiru branch. Its arrival faces north across a 112 x 96 obstruction-free surrender battlefield toward a 112 x 96 castle, while the normal mid-tier ascent tower remains directly behind the player at the south edge. The castle uses sparse 32-block tiles, contains one open great hall, courtyard, throne position for Esil, resident anchors, and six clear training-dummy pads, and avoids narrow sealed rooms. A separate 20 x 8 portcullis overlay lets the encounter open or restore the outer gate without replaying any castle tile.
- `DkcFloorBuilder` is the coordinate authority for House Radiru. Runtime systems consume its stable Esil, resident, wave, training, courtyard, castle, and gate helpers instead of duplicating local offsets. The large castle and field contain no block entities or server loops, are generated once under the same 16 x 16 per-tick placement budget, and unload naturally with the rest of the Floor-15 cell.
- The deterministic generators are `tools/generate_dkc_dimensions.py` and `tools/generate_dkc_structures.py`. The dimension generator emits one shared LevelStem/type, nineteen compatibility-only DKC type stubs, and twenty legacy-safe biome files while removing retired floor LevelStems.

## Coordinate-driven visual progression

- `DkcDimensionSpecialEffects` selects one of 20 visual profiles from the camera's floor cell.
- Every floor can still have distinct fog color and range, sky palette, lightmap tint, and ambient particles while using the same dimension type and server realm.
- Three bounded, untextured sky families provide ember, furnace, and upper-tower tempest looks. Their geometry is only a few hundred client-side vertices and does not require a server particle loop or per-floor shader resource.
- Every floor shares a huge blood-red rendering of Minecraft's vanilla full-moon texture, fixed at an exact 45-degree elevation. It faces the +Z tower horizon on climbing floors and flips to -Z over Radiru Castle on Floor 15. One textured quad and a restrained client-only halo replace the old procedural body/crater discs; the quad uses vanilla's additive celestial blend because `moon_phases.png` stores opaque black around each phase, and its scale compensates for the visible full moon occupying only part of the 16 x 16 atlas cell. It never consults world time and adds no server tick or dimension cost. A DKC-scoped client mixin suppresses the ordinary sky/cloud path, while the custom vault renders at Forge's `AFTER_SKY` stage so renderer replacements cannot restore the small overhead white moon.
- Ambient particles progress from ash to white ash to crimson spores, respect the client's particle setting, and are capped at one or two spawns per scheduled client tick.
- Every floor owns one `DKCTowerAuraEntity` configured through persisted NBT-backed `Radius`, `Height`, `Intensity`, and `CrownLightning` properties. Floor 1 keeps the full `32 / 320 / 0.86 / true` profile; later tiers use heights from 96 to 192, intensities from 0.58 to 1.0, and enable lightning only on upper floors.
- Aura creation is owner/floor-idempotent, waits until its anchor chunk is entity-ticking, and retries only every five seconds while the player occupies that floor. Cells are 2,048 blocks apart and aura tracking/render distance is 512 blocks, so only the current floor's effect can be visible or ticking and no aura forces a chunk ticket.

## Encounter rules

- Floor 15 converts its normal completion into a durable branch. Overpowering the bounded battlefield wave opens only the Radiru portcullis and spawns owner-bound surrendered Esil inside the keep; it does not award or clear the floor until the owner right-clicks Esil for the permit or kills her.
- Accepting Esil's permit persists the Radiru pact before awarding Floor-15 XP and the item, changes Esil and surviving residents into protected sanctuary NPCs, and repopulates missing residents only while the owner occupies the cell. Killing Esil or any surrendered resident instead grants the owner-bound Floor-15 permit, bonus XP, and the forbidden Cold Blood Runestone but permanently forfeits the Radiru route. Esil can replace a lost permit only until Floor 15's transition is durably armed, and its floor/owner NBT prevents it from opening later pedestals.
- Six tagged training demons use existing Demon/Demon Knight entity types, no AI, no natural spawning, fixed stations, 1,024 health, and six armor/toughness profiles. Lethal damage is canceled, every hit starts from full health, and the owner receives rate-limited final post-mitigation hit and rolling-DPS values; training hits are excluded from mastery/progression and combat-lock rewards while ordinary damage procs remain available for measurement.
- Radiru recovery treats player capability outcome as authoritative, verifies the completed surrender wave before either branch, repairs blocks/entities on build, entry, and login, waits for each actor anchor's entity-ticking chunk, and deduplicates by station. Layout migration carries a one-shot cleanup bit so only previously generated Floor-15 cells receive a sliced above-foundation scrub before the sparse castle is placed; brand-new cells pay no cleanup scan.
- A pact owner who defeats Floor 20 automatically unlocks `A House Beyond the Gate`. The System Quests button becomes `Radiru Castle`, directly returns the player to their unloaded-until-used Floor-15 cell, and seals/disables the old ascent trigger while leaving the tower and aura as scenery. Old completed saves without a Floor-15 outcome are migrated to the non-destructive pact route.

- Normal floors use staged waves with a floor-specific total and only 7–12 simultaneous enemies.
- Ordinary demons have an 8-12% promotion roll until at most one Elite Demon has appeared in that floor attempt. Promotion consumes no extra spawn slot or AI loop and persists its larger model/hitbox, health/damage multipliers, armor, toughness, knockback resistance, Resistance I, and elite role through saves.
- Every runtime enemy and boss carries `dkc_floor_number`, `dkc_spawned_by`, and an encounter role.
- Floor-wave mobs are created and tagged before world insertion, then admitted only to loaded, hazard-free 3 x 3 x 3 pads inside the authored district when their exact hitbox fits, they have direct sight of the owner, and vanilla ground navigation can reach the owner. There is no unchecked fallback position.
- Persistent wave mobs validate their owner and attempt once per second. Out-of-footprint mobs are recovered immediately; only mobs that make no useful progress for ten seconds receive a path check and possible relocation, keeping the steady-state cost to cheap coordinate/tag checks.
- Wave-attempt IDs prevent unloaded enemies from an earlier death, logout, or completed objective from returning and counting toward a later run. Ownerless and stale mobs discard lazily without forcing their chunks to load.
- Only the `floor_wave` role advances normal objectives. Baran's `boss_add` knights never count.
- Entity searches are bounded to the owner's exact floor cell. Boss/add existence, cleanup, highlights, and progression are owner-scoped.
- Baran can keep at most eight active adds, and those adds are removed when Baran falls or the attempt resets.
- Floor 10 requires its guard objective before Vulcan appears.
- Floor 20 requires both independently tracked boss deaths before completion and rewards. The Kaisel soul is owner-bound and appears only after that joint objective completes.
- A failed boss attempt discards the owned bosses and spawns clean instances on retry, avoiding leaked AI phases or boosted attributes.
- Boss recovery runs only when the authored anchor chunk is entity-ticking. A short owner/floor/type spawn guard closes the old hidden-chunk race, tags a new boss before insertion, retains one exact-type owned/legacy candidate, and discards duplicates deterministically; exact entity-type filtering prevents Shadow Kaisel from being mistaken for floor-20 Kaiselin.

## Security, migration, and recovery

- Path-menu packets require an actual open Path menu, a started DKC run, a server-unlocked floor, and an Overworld sender.
- The permission-level-3 `/slr <targets> dkc <floor>` test route sets `dkc_cleared` to `floor - 1`, rewrites unlocked/armed saved-run bits, clears the selected encounter attempt, closes its physical transition, and teleports only after that floor is generated. A latest-request marker prevents an older asynchronous build from pulling a tester back, while boss replacement waits for the relevant anchor chunk instead of installing a ticket.
- Permit interaction validates the exact owner-relative pedestal, current-floor completion, held permit, and transition state.
- Survival/adventure players cannot break or place blocks in the shared DKC realm; creative and spectator administration remain available.
- Login recovery handles both the original Z-stacked layout and all nineteen retired floor dimensions, then resumes interrupted generation in the player's assigned sector.
- Delayed generation and recovery work is bound to its originating `MinecraftServer`. Server stop or player disconnect cancels an active build, so stale callbacks cannot cross integrated-server sessions and an interrupted floor simply resumes later.
- Saved `dkc_started` state records progression only. Server-side current-DKC authorization additionally requires `dkc_inside_castle` and a coordinate-resolved floor owned by that player.
- Invalid or duplicate persisted slots are reallocated with their generated-floor bits cleared, ensuring the newly assigned empty sector is rebuilt instead of being mistaken for finished terrain.
- Layout version 5 invalidates generated bits from the earlier shared-realm layout so existing floors receive the tower segments, outside pedestals, and corrected open ruined-cathedral entrance without changing unlock or permit claims.
- Cross-dimension transfers reset motion and fall distance and do not persist a temporary no-gravity state.
- Return coordinates include dimension, position, yaw, and pitch, with an Overworld spawn fallback.

### Offline migration for worlds created with the old 40-LevelStem build

Minecraft persists the complete LevelStem map in `level.dat`. Compatibility-only dimension types, biomes, and noise settings let an old world open, but that world will continue constructing its 27 retired `ServerLevel` shells until its saved map is migrated. The opt-in maintenance task removes only `dungeon_dimension_dkc_f02` through `_f20` and the eight `monarch_territory_*` keys from `Data.WorldGenSettings.dimensions`. It does not delete or move any dimension folder.

Stop the server and make a separate world copy first. Dry-run is the default:

```powershell
.\gradlew.bat migrateLegacyLevelStems "-PlegacyWorld=E:\path with spaces\world"
```

Review the exact 27 keys printed by the dry-run, then apply explicitly:

```powershell
.\gradlew.bat migrateLegacyLevelStems "-PlegacyWorld=E:\path with spaces\world" -PapplyLegacyStemMigration=true
```

The task refuses a world with an active or unavailable `session.lock`, creates a timestamped `level.dat.before-shared-realms-*.bak`, verifies the rewritten NBT before replacement, and requires an atomic same-directory move. Keep the backup until the migrated world has completed a clean startup and shutdown. The retired dimension folders are deliberately preserved for manual archival after validation.

The maintenance task was dry-run and applied to a disposable copy of a legacy validation world. It removed exactly 27 retired keys, reduced the saved map from 43 total stems to 16, preserved every dimension folder, and produced a byte-for-byte backup of the original `level.dat`. The migrated copy subsequently reached the dedicated server's `Done` state without dimension, codec, or datapack errors.

## Shared Red Gate application

The same principle is also used for Monarch Red Gates:

- All eight territory arenas now occupy `sololeveling:dungeon_dimension_snow` instead of eight territory dimensions.
- `RedGateRealmLayout` uses 1,024-block cells. The X lane identifies one of eight territories and the Z lane identifies one of up to 256 active/reusable instance slots.
- Persisted active centers prevent slot collisions. Cell cleanup is bounded and does not load old cells just to clean them.
- The shared realm surface is Y=32, safely above the generic dungeon void-rescue threshold.
- The flat generator is only a cheap hidden substrate. First entry deterministically sculpts a 193-block-wide landscape around the allocated center, holding a safe central clearing inside rolling territory-specific terrain and scenery before feathering back to substrate outside the 80-block playable boundary; the old raised circular platform is no longer exposed.
- The shared client effect selects territory fog, twilight, aurora, and moon colors from the X lane.
- Retired territory dimensions are recovery-only; an old active battle is closed safely instead of trying to migrate live wave entities. Their old noise settings are retained solely as codec dependencies for existing saves and do not register or tick a dimension.
- Arena preparation is reconstructed from durable instance state and advances under one 256-column-operation budget shared fairly across runnable jobs each server tick. It pauses when no bound participant is online, skips unchanged writes, uses block-update flag `2`, and separately caps scenery and perimeter props per tick so the richer terrain does not become a generation spike.
- Preparation failure removes the invalid instance, safely recovers or clears entrants, and rolls back the source gate when it is loaded. Completed return-portal repair likewise requires a bound participant and an already-loaded exit chunk.

## Reusing this pattern

For another multi-stage dungeon, reuse the same separation of concerns:

1. Register one cheap, featureless realm for content that shares basic world physics.
2. Allocate a stable per-player or per-party coordinate slot with generous empty spacing.
3. Derive theme or stage from coordinates and validate ownership separately.
4. Persist `claimed`, `generated`, and `open` as separate states.
5. Claim consumable access before starting asynchronous work.
6. Generate deterministic, idempotent modules in a staged queue.
7. Tag every encounter entity with instance owner, stage, and role.
8. Use bounded scans, hard active-entity caps, and no permanent chunk tickets.
9. Make physical world interaction trigger travel; treat GUIs as convenience, never authority.
10. Add login, death, server-stop, and retired-layout recovery before adding more content.

## Verification

- Static geometry audits cover every one of the 4,096 DKC slots x 20 floors (81,920 cells) and all eight Red Gate lanes x 256 slots (2,048 cells), with no coordinate collisions or reverse-mapping failures.
- Resource audits require one DKC dimension, one shared Red Gate dimension, and no retired DKC-floor or Monarch-territory LevelStem in the built resources. Compatibility-only types, biomes, and noise settings are expected because old saves reference them while decoding.
- The full Java 17 release build and jar audits pass with 772 procedure references checked, 3,695 classes packaged, exactly 13 live custom LevelStems, one DKC realm, one shared Red Gate realm, and no retired floor/territory LevelStem in the jar.
- A fresh shared-realm world saved exactly 16 total stems (13 mod plus three vanilla), and both that world and the copied/migrated legacy world reached the dedicated server's `Done` state without dimension, codec, or datapack errors. The fresh world also reopened successfully after the validation harness terminated it without a console `stop` command.
- After the hold-Tab tracker integration, a new disposable Java 17 server reached `Done` in 30.423 seconds without startup-fatal or client-classloading errors; that 40,843,538-byte production jar was installed byte-for-byte in the CurseForge test instance.
- After the exact-floor command integration, the release audits still package 3,695 classes and a fresh Java 17 server registers the expanded command tree before reaching `Done` in 31.525 seconds. The current 40,846,698-byte production jar is installed byte-for-byte in the CurseForge test instance.

Full client gameplay, 20-floor and party concurrency, live-player migration/progression, balance, and visual tuning still require in-game playtesting.
