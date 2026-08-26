# NeoForge 1.21.1 Migration and Mechanics Review

## Purpose

This document records the Forge 1.20.1 to NeoForge 1.21.1 migration and turns issues found during the port into a practical mechanics-improvement backlog. The migration was performed in the separate `slbc-neoforge-1.21.1` project; the original `slbc22052025` project is the preservation baseline.

The project is unusually large for a single mod: approximately 2,595 Java files, 284 entity classes, 1,159 generated procedure classes, 99 network-message classes, 1,148 JSON resources, and 60 shader programs. Recommendations below therefore favor central services and data-driven definitions over more duplicated generated code.

## Migration inventory

### Platform and build

- Java 21, Gradle 9.2.1, ModDevGradle 2.0, and NeoForge 21.1.x.
- GeckoLib moved to its NeoForge 1.21.1 artifact and current 4.x render/controller APIs.
- Forge event, registry, capability, networking, lifecycle, attachment, item-component, saved-data, and world-generation APIs were ported to their NeoForge/Minecraft 1.21.1 forms.
- Resource locations, holders, data components, attribute modifiers, effect holders, sounds, menus, block entities, entity synchronization, and data-pack directory names were migrated.
- Legacy numeric packets are carried by a NeoForge custom-payload adapter so the existing message dispatch model can be retained during this port.

### Sodium and Iris compatibility

- Oculus/Embeddium-specific integration was removed from the copied project.
- Sodium and Iris are optional runtime integrations; the mod must still function without either one installed.
- Custom sky rendering uses the dimension-effects callback expected by Iris custom-sky handling.
- Deferred world VFX are replayed after level rendering under Iris to avoid invalid depth copies and model-view state conflicts.
- Legacy immediate-mode vertex calls were migrated to the 1.21 rendering APIs, and shader assets were audited for resource-reference consistency.

Important limitation: effects drawn after Iris's final composite can be made visible and stable, but they cannot retroactively participate in shader-pack shadow maps, reflections, or earlier deferred passes. Effects that require those features should eventually be represented as ordinary world geometry in the correct render stage or exposed through a dedicated Iris integration when one exists.

### Save and content compatibility

- Player variables use a NeoForge attachment with a compatibility facade for the existing call sites.
- Item-owned state uses 1.21 data components, with a bridge for legacy nested item data where practical.
- SavedData and block entities now use registry-aware load/save signatures.
- Entity synchronized data definitions and spawn initialization use the 1.21 builder-based APIs.
- Recipes, advancements, loot tables, tags, predicates, functions, structures, biome modifiers, particles, models, textures, and shaders were moved or validated for 1.21 resource paths.

World upgrades should still be tested on disposable copies. Attribute modifier UUIDs are now stable resource IDs, and some vanilla/NeoForge data fixers may not automatically remove every old serialized modifier identity.

### Runtime migration findings

- A clean NeoForge dedicated-server smoke test reached `Done` with no `ERROR` or `FATAL` entries after validating every custom dimension, data-pack function, painting variant, tag, and biome spawn placement.
- Sodium 0.6.13 + Iris 1.8.12 passed both a shaders-disabled smoke test and an active MakeUp Ultra Fast 9.5c shader-pack run. The active run selected the pack, created and reloaded the overworld pipeline, joined an integrated world, rendered the Solo Leveling HUD and System panel, activated the deferred Iris compatibility path, logged out, and saved every dimension with no `ERROR`, `FATAL`, shader compile/link failure, or fallback entry.
- Sodium 0.8.12 is accepted as an optional renderer because the mod does not link to Sodium APIs. The current Iris 1.8.14 beta advertised for Sodium 0.8 was also tested, but that pair fails during Iris bootstrap before Solo Leveling loads: Iris references Sodium's absent `VertexSerializer` class and numerous old mixin targets. The development run therefore remains pinned to the proven stable 0.6.13/1.8.12 pair until Iris publishes a mutually compatible 1.21.1 build.
- The old `run/debug/disconnect-2026-08-06_03.55.17-client.txt` report was a downstream `ClosedChannelException`. The initiating failure was 1.21 rejecting `ItemStack.EMPTY` through the non-optional codec. All ten persisted/synchronized stack fields now use the optional 1.21 codec; a fresh join and logout produced no new debug report.
- Minecraft 1.21 moved the background pass into `Screen.render` and `AbstractContainerScreen.render`. Retained 1.20-style explicit calls caused a second blur/dim pass that covered custom text and graphs. The one-pass fix covers 59 concrete screens through direct edits and shared responsive bases, including the System settings, containers, world-creation odds editor, Guild UI, and Shadow UIs. A source-contract regression now guards the render order.
- System notifications now use the normal world stage without a shader pack and the post-composite `AFTER_LEVEL` pass when Iris has a pack active. Their billboard orientation matches the 1.21 name-tag convention and text has a depth-independent readable layer.

## Mechanics improvement backlog

### P0 — correctness and exploit resistance

1. **Make abilities server-authoritative.** Define cooldown, mana cost, range, target validation, and state transitions once on the server. Packets should express player intent only. Reject impossible casts and rate-limit repeated inputs. This closes desync and packet-spam exploits while simplifying all 99 message handlers.
2. **Version every persistent subsystem.** Add explicit data versions and one-time migrations for player progression, guilds, shadow inventory, titles, dungeon state, and legacy attribute UUIDs. Log migration summaries without logging player-sensitive data.
3. **Centralize the combat pipeline.** Route outgoing damage, incoming mitigation, critical hits, class passives, party rules, Better Combat hits, invulnerability frames, and kill credit through a small ordered pipeline. The present spread across generated procedures and event handlers makes double application and ordering bugs likely.
4. **Guarantee dungeon cleanup.** Model a dungeon as a persisted state machine (`creating`, `active`, `boss`, `reward`, `closing`, `failed`) with idempotent cleanup. On server restart, either resume safely or roll back spawned entities, tickets, barriers, temporary blocks, and player locks.
5. **Bound all stored collections.** Put explicit caps and validation on shadow inventories, queued summons, party invitations, pending notifications, delayed server work, and dungeon markers. Reject malformed or oversized NBT/component payloads.

### P1 — combat feel and balance

1. **Use data-driven ability definitions.** Store base damage, scaling stat, mana cost, cooldown, cast time, hit shape, knockback, sound profile, and VFX profile in reloadable definitions. Code should implement reusable targeting and effect primitives. This makes balance patches possible without editing dozens of procedures.
2. **Introduce clear hit phases.** Use `wind-up → active → recovery`, with explicit cancel rules and short, consistent hit-stop. Telegraph boss attacks by danger rather than by arbitrary animation duration. Better Combat animation timing should feed the same server hit window instead of creating a second damage path.
3. **Normalize crowd control.** Give stun, freeze, knock-up, pull, and fear a shared resistance/diminishing-return model. Bosses can have partial resistance rather than blanket immunity, keeping control builds useful without allowing permanent locks.
4. **Replace periodic modifier churn.** Apply title, guild, class, and equipment modifiers when state changes, not by removing and recreating them every tick. A single attribute service should own stable IDs and idempotent updates.
5. **Improve projectile consistency.** Define projectile speed, acceleration, gravity, lifetime, collision width, owner immunity, and maximum range in one profile. Add server-side swept collision tests for very fast projectiles and test the 1.21 acceleration behavior independently.
6. **Separate damage identity from presentation.** Give each ability a stable damage-type/tag identity, then choose particles, camera shake, impact frames, and sounds client-side. This makes resistances and advancement criteria reliable even when visuals are reduced.

### P1 — shadows and companions

1. **Use a shared shadow brain/state machine.** Recommended states are `follow`, `guard`, `attack`, `return`, `downed`, and `dismissed`. Store command state separately from transient navigation state so reloads do not strand summons.
2. **Budget pathfinding and ticking.** Update distant followers less often, teleport only after a configurable stuck timeout, and stagger expensive target searches across ticks. Avoid every shadow scanning a large area on the same tick.
3. **Make equipment validation explicit.** Use typed components for shadow slot rules, ownership, and upgrades. Clamp inventory size and reject recursive containers or invalid copied stacks.
4. **Improve tactical commands.** Add formation distance, focus-fire, passive/defensive/aggressive stance, and a visible recall status. Commands should acknowledge success/failure so the client HUD cannot lie about server state.
5. **Preserve summon identity.** Give long-lived shadows a stable persistent ID and migration version. Reconcile duplicates on login or dimension change rather than relying only on nearby-entity searches.

### P1 — dungeons, gates, and bosses

1. **Deterministic generation.** Persist a dungeon seed plus selected room IDs. Rebuilding the same run should produce the same topology, which makes recovery and bug reports reproducible.
2. **Validate data packs before activation.** Produce actionable errors for missing pools, unreachable exits, invalid markers, excessive room counts, unknown entities, circular references, and impossible boss/reward layouts. Keep the previous valid snapshot if reload validation fails.
3. **Scale by party capability, not only headcount.** Use a capped combination of party size, effective level, equipment tier, and role coverage. Scale health less aggressively than damage pressure to avoid long health-sponge fights.
4. **Use encounter budgets.** Each room should have a threat budget and simultaneous-mob cap. Spend it on varied roles and reinforcement timing instead of spawning every configured mob at once.
5. **Telegraph boss state.** Expose stagger, enrage, phase thresholds, interruptibility, and target swaps through consistent HUD and animation language. Make unavoidable damage exceptionally rare and clearly signposted.
6. **Add recovery commands and diagnostics.** Administrators need a safe command to inspect a run, return trapped players, retry cleanup, or close a corrupt instance without deleting unrelated world data.

### P1 — natural spawning and entity ecology

1. **Do not make normal biome spawns permanently persistent.** Sixteen of the seventeen custom hostiles added through biome modifiers currently call `setPersistenceRequired` and also refuse distance-based removal. Keep persistence for named, summoned, scripted, or dungeon-owned instances, but let `MobSpawnType.NATURAL` instances despawn normally. Otherwise explored terrain can retain thousands of mobs and permanently saturate mob caps.
2. **Use per-family spawn rules.** A shared hostile ground rule is a safe migration baseline, but flying Fire Flies, very large Centipedes/Golems, and ordinary humanoids should eventually have different clearance, altitude, surface, light, group-size, and density rules.
3. **Test spawn registration coverage.** Compare every entity referenced by biome modifiers with `RegisterSpawnPlacementsEvent` registrations in an automated test. Also test peaceful difficulty, invalid floors, crowding, and dimension-specific light behavior.

### P2 — progression and economy

1. **Consolidate progression curves.** Put level XP, stat gains, mastery, class progression, title requirements, guild buffs, and dungeon rewards in versioned data. Generate a balance table in CI to catch discontinuities or negative values.
2. **Add soft caps and meaningful choices.** Use diminishing returns for extreme attack speed, cooldown reduction, defense, summon count, and movement speed. Prefer mutually interesting build choices over universally best linear stat stacking.
3. **Protect reward transactions.** Treat shop purchases, guild storage, dungeon rewards, and upgrade costs as atomic server transactions: validate, remove cost, grant result, then persist. Include rollback behavior if the destination inventory is full.
4. **Make rarity legible.** Standardize rarity colors, affix counts, stat ranges, salvage value, and upgrade ceilings. Provide exact numeric comparisons in tooltips rather than relying only on color or lore.
5. **Test party reward rules.** Define distance, contribution, death, disconnect, rejoin, and late-join behavior for XP and loot. Apply the rule once from a central reward service.

### P2 — performance, rendering, and accessibility

1. **Create a VFX budget manager.** Cap particles, trails, screen overlays, lights, and deferred effects per frame and per ability. Reduce density by distance before removing important telegraphs. Offer `low`, `medium`, and `full` presets.
2. **Replace command-per-particle lightning.** The eight generated lightning functions currently contain 15,706 `dust` particle commands; the seven functions that needed 1.21 numeric repairs account for 14,178 of them, and `purple_lightning_1` alone emits 5,102 commands. Replace these with a compact parametric or batched client VFX message, apply distance/LOD gating, and stop using `force @a` for every particle.
3. **Prefer batched world rendering.** Reuse buffers and render types, frustum-cull large effects, avoid allocations inside render loops, and cache immutable geometry. Keep render code independent of Sodium internals.
4. **Treat shaders as optional enhancement.** Every mechanic must remain readable with vanilla rendering, Sodium alone, Iris with shaders off, and Iris with a shader pack. Avoid gameplay-critical information encoded only in bloom or emissive passes.
5. **Expose accessibility controls.** Add sliders/toggles for camera shake, hit flash, impact frames, particle density, aura opacity, HUD scale, and high-contrast telegraphs. Never bind critical state solely to red/green distinctions.
6. **Centralize sound profiles.** Abilities should reference a sound profile containing holder, source, volume, pitch variation, attenuation, and cooldown. This prevents duplicated sound calls and makes loudness balancing practical.
7. **Keep text pixel-aligned.** Reflow large panels before applying fractional pose scaling, render help text once at its final transform, and test GUI scales 1-4/Auto at 1280×720 and 1920×1080. This avoids texture-filter blur and catches clipped explanatory rows.

## Recommended test matrix

### Automated

- Clean `compileJava` and full Gradle `build` on Java 21.
- JSON/resource-reference validation and shader compilation/audit.
- Headless data-pack reload validation for generated functions, including homogeneous SNBT numeric-list types and painting-variant/tag resolution.
- Biome-spawn-to-spawn-placement coverage and natural-despawn behavior.
- GameTests for attachment cloning, item-data migration, modifier idempotence, damage ordering, party rewards, shadow reconciliation, dungeon reload/recovery, and block-entity round trips.
- Packet tests for direction, authorization, invalid entity IDs, oversized data, and rate limiting.
- Dedicated-server startup and data-pack reload with no client classes loaded.
- GUI render-order checks that reject the old explicit-background-before-`super.render` pattern, plus screenshot comparisons for the world settings, odds graph, System panels, Guild/Shadow containers, and notification text at multiple GUI scales.

### Manual client matrix

1. NeoForge + GeckoLib only.
2. NeoForge + GeckoLib + Sodium.
3. NeoForge + GeckoLib + Sodium + Iris, shaders disabled.
4. NeoForge + GeckoLib + Sodium + Iris, representative shader pack enabled.
5. The above with Better Combat installed and absent.

For each case, verify login/respawn/dimension change, HUDs, menus, every class ability, melee timing, projectiles, boss phases, shadow commands, guild storage, each dungeon family, death/recovery, resource reload, and reconnect after a forced server restart.

## Suggested implementation order after the port

1. Save-versioning and migration tests.
2. Server-authoritative ability and packet validation layer.
3. Central combat/attribute/effect pipeline.
4. Dungeon lifecycle recovery and deterministic generation.
5. Shadow AI/tick budgeting.
6. Data-driven ability/balance definitions.
7. VFX budgeting and accessibility controls.
8. Broader automated GameTest coverage.

This order protects existing worlds and multiplayer correctness first, then improves maintainability, balance iteration speed, performance, and presentation.
