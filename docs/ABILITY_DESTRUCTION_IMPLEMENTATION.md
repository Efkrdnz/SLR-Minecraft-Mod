# Ability Destruction: Design and Implementation

## Goal

Ability destruction is an opt-in terrain-damage layer for attacks whose fantasy
depends on visible environmental force. It is deliberately not attached to every
damaging skill. Large impacts, beams, fissures, and finishers may alter terrain;
mobility, healing, summons, defenses, precision attacks, and rapid repeat effects
remain terrain-safe.

The system is server-authoritative and uses bounded block-removal jobs instead of
vanilla block-damaging explosions. Entity damage and visual effects remain owned by
the ability managers; terrain work is submitted separately to
`AbilityDestructionManager`.

## World setting and gamerule

- Gamerule: `soloAbilityDestruction`
- Category: Player
- Default: `false`
- Runtime command: `/gamerule soloAbilityDestruction true|false`
- World creation: the full-width **Ability Destruction** toggle appears directly
  below **Story Mode** in the **Solo Leveling** settings tab. It reads and writes the
  world-creation `GameRules` instance and resynchronizes if those rules change.
- Demon King Castle dimensions always reject ability-destruction jobs, even when the
  gamerule is enabled, so instance progression cannot be bypassed.

The retired `SOLO_WORLD_GRIEFING` Java field is only a source-compatibility alias for
the new key. There is one registered setting, and no startup procedure forces it on.

## Destructive ability matrix

| Family | Terrain-enabled attacks | Shape and scaling driver | Deliberately terrain-safe |
| --- | --- | --- | --- |
| Antares | Destruction Claw, Breath of Destruction, Monarch's Descent, Sovereign Roar, Extinction | Claw line plus a separate large finisher impact (`Strength + 0.32 Intelligence`); obstruction drilling for Breath (`Intelligence + 0.55 Strength`); Descent crater (`Strength + 0.32 Vitality`); full-radius ring and radial fractures for Roar (`Strength + 0.48 Intelligence`); deep pulsed Extinction tunnel plus endpoint crater (`Strength + 0.75 Intelligence`) | Monarch Manifestation does not damage terrain by itself; it empowers the attack profiles |
| Goliath | Power Smash, Collapse, Pursuit | Power Smash uses a shallow Strength-scaled surface fissure, Collapse uses broad craters while preserving a footing island, and Pursuit separates a small travel-clearing profile from its large landing impact | All Enhanced Strike left clicks (including the combo finisher), Capture, and non-impact stance/utility behavior |
| Beast Monarch | Claw Rift, Rubble Jaw | Strength-scaled fissure and impact; White Fang Sovereign supplies the empowered flag | King's Maul, Feral Reconstitution, and the Sovereign transformation itself |
| Fire Mage | Ignition Orb impact, Furnace Dominion finale, Heavenfall's central meteor, charged legacy fire beam | Intelligence-scaled impacts or clipped line; stage 4/5 and high charge can empower the profile. Legacy charged beams use dedicated profiles so they cannot inherit ultimate-sized craters | Flame Weaving, Inferno Lance, Flashfire, Cremation, Furnace pulses, Heavenfall follow-up bursts, and low-charge fire release |
| Storm Mage | Thunderhead's final strike, Skybreaker primary detonation | Intelligence-scaled impact; Overcharge or high output stage empowers it | Static Needle, Slipstream, Thunderclap, Lightning Rod, Chain Lightning, Tempest Incarnate, earlier Thunderhead strikes, and Skybreaker echoes |
| White Flame Monarch | Lightning Breath, limited Hellstorm Dominion lightning strikes, Radiru Blood Spear on block collision | Intelligence-scaled line/impacts; Spiritualization empowers them. Breath clips at the first wall so repeated pulses drill progressively, and Hellstorm is capped to four normal or six spiritualized terrain strikes per cast | Doppelganger, Hell's Army, Spiritualization itself, entity-only spear hits, and projectile expiry |
| Frost Monarch | Ice Spear when it anchors into a block | Small impact driven by `Intelligence + 0.55 Strength`; Spiritualization empowers it | Flash Freeze, Frost Counter, Absolute Zero, Stillness Decree, Pale Causeway/Frozen Path, Winter Remembers, Whiteout Procession, and Spiritualization itself |
| Arcane Mage | Primary Dimensional Rend, Grand Formula: Convergence finale | Intelligence-scaled line and impact; Overcast/high stage empowers them. Delayed side rends do not submit additional terrain jobs | Aether Bolt, Vector Step, Polarity Sphere, Runic Relay, Astral Arsenal, repeated convergence pulses, and delayed bursts |
| Barrier Mage | Resonant Collapse | Intelligence-scaled impact. Stages below 5 use at most three construct centers; stage 5 uses one empowered averaged center with a dedicated catastrophe profile | Fracture Bolt, Prism Rampart, Repulsion Frame, Sealing Prism, Mirror Ward, Absolute Bastion, and all persistent defensive constructs |
| Tanker | Tank Leap landing | Impact driven by `Strength + 0.5 Vitality + 14 x attack damage` | Taunts, marks, reinforcement, defensive stances, bash-style precision hits, and willpower effects |
| Fighter/ranker baseline | Ground Slam and the legacy Sword Beam projectile on block collision | Strength impact; Sword Beam adds `10 x attack damage` and carves only around the collision point | Cross Strike, Rush Attack, Sword Dance, buffs, and other rapid or precision attacks |
| Liu Zhigang | Sword beams when a non-execution beam collides with terrain | Short Strength-scaled sword cut at the collision; tier 2+ or dual-wield beams are empowered | Execution beams and attacks that do not strike a block |
| Grand Marshal preview | Igris: Crimson Cross; Tusk: Gravitational Ruin; Kamish: Dragon's Dread; Kaisel: Sky Rend | Strength, Intelligence, hybrid magic/physical, or Agility hybrid scaling respectively, with shadow power folded into the same driver; high shadow rank empowers dedicated profiles rather than borrowing player-ultimate profiles | Beru: King's Restoration and all healing/summon-management behavior |

NPC spell dispatch does not create terrain jobs because the destruction entry points
require a `ServerPlayer`. This keeps the gamerule focused on player abilities rather
than turning every generated hunter encounter into world griefing.

## Scaling model

Each destructive call supplies the attribute expression already associated with the
attack's damage fantasy. The shared manager converts it into a bounded normalized
factor:

```text
normal = clamp(log1p(max(0, drivingAttribute)) / log1p(profileCeiling), 0, 1)
empowered = min(1, 0.20 + normal * 1.02)
```

That factor linearly interpolates three independent values within the selected
profile's fixed minimum and maximum:

1. Block mutation budget: how many eligible blocks the job may remove.
2. Radius/width: the size of the impact, line, fissure, or ring.
3. Maximum hardness: stronger attacks can affect tougher ordinary terrain.

The logarithmic curve keeps early investment visible while allowing high-stat builds
to continue gaining terrain scale instead of saturating at 200. Profile ceilings are
350 for ordinary attacks, 500 for major spell/Grand Marshal profiles, 600 for
Goliath and Beast profiles, and 1,200 for Antares. Empowered states provide a soft
floor and acceleration but never exceed each profile's hard cap. Invalid or
non-finite attributes are treated as zero.

## Antares treatment

Antares has the widest and hardest profiles, but each attack uses a different shape
so the results read clearly instead of becoming repeated spherical holes:

| Attack | Profile envelope | Behavior |
| --- | --- | --- |
| Destruction Claw | 80-280 blocks, width 2.0-4.5, hardness 15-55; finisher 300-900 blocks, radius 5-10, hardness 25-75 | A forward cut; the Ruin finisher uses its own broad endpoint impact |
| Breath of Destruction | 100-360 blocks, width 2.5-5.5, hardness 20-70 | Each pulse drills a short segment around the current obstruction instead of clearing the whole visual range at once |
| Monarch's Descent | 500-1,800 blocks, radius 8-16, hardness 35-90 | A broad, vertically shallow crater submitted only after a confirmed ground/wall collision; a small footing island remains beneath the caster and a midair timeout stays terrain-safe |
| Sovereign Roar | 320-1,000 blocks, radius 8-14, hardness 30-80 | The annulus reaches the actual visual radius and adds eight shallow radial cracks |
| Extinction | 260-700 blocks per pulse, width 3.5-7, hardness 40-100; endpoint 500-1,400 blocks, radius 7-14 | Three locked-direction drilling pulses. Normal depths are 6/12/18 blocks; manifested depths are 8/16/24. The last pulse is empowered and creates an endpoint crater |

Extinction can therefore request the largest total terrain change, but it still obeys
the global tick, queue, protection, hardness, loaded-chunk, and expiry limits. The
locked direction and incremental re-trace let later pulses advance only after earlier
terrain has been cleared. Manifestation has no ambient block loop; its effect is to
strengthen these explicit attacks.

## Shapes and bounds

- **Impact:** a shallow ellipsoid whose surface footprint is selected before its
  deeper core, so large configured radii remain visible instead of becoming narrow
  center holes. Self-centered Goliath Collapse and Antares Descent preserve a small
  footing island.
- **Line:** a bounded cylindrical cut between two finite points.
- **Fissure:** a shallow surface trench, independently capped to at most three
  blocks of depth; it never delegates to the full three-dimensional line cutter.
- **Ring:** a shallow full-radius annulus with eight bounded radial cracks rather
  than a cleared disk.
- Ring input radius is capped at 24 blocks.
- Most line requests are capped at 32 blocks. Tighter caps apply to Antares Breath
  (6), Antares Claw (12), Goliath Pursuit travel (5), and Beast Claw/Liu sword cuts
  (24). Extinction may drill up to 26 blocks.
- Candidate ordering includes deterministic position/owner jitter. This keeps edges
  organic while producing stable server results.

## Performance contract

Terrain requests become jobs and are processed at the end of server ticks. The
limits are global to the server unless stated otherwise:

| Limit | Value |
| --- | ---: |
| Successful block mutations per tick | 96 |
| Candidate inspections per tick | 512 |
| Successful mutations from one job per tick | 48 |
| Queued positions | 24,576 |
| Queued jobs | 128 |
| Queued positions from one player | 6,144 |
| Queued jobs from one player | 24 |
| Shape requests from one player per tick | 4 |
| Shape requests globally per tick | 24 |
| Candidate positions retained per job | Scaled mutation budget plus a bounded 12-256 position validation reserve |
| Job lifetime | 200 ticks |

Queued positions are deduplicated by dimension and block position, preventing
overlapping pulses from multiplying the same work. Per-player queue and per-tick
request quotas prevent creative cooldown bypass from monopolizing candidate
generation or the global queue. Jobs are round-robin processed, recheck the gamerule
before every mutation, and are discarded when the casting player dies/respawns,
disconnects, changes dimension, the rule is disabled, or the expiry is reached.
Server-stop cleanup clears all pending jobs.

Only already-loaded chunks are inspected or mutated. Build limits and the world
border are checked, and no code path forces a chunk load. The manager creates no item
drops and does not use vanilla terrain-damaging explosions.

## Protection and eligibility

A candidate block is rejected when any of the following is true:

- It is outside build height, outside the world border, or in an unloaded chunk.
- It is air, contains fluid, has a block entity, is unbreakable, exceeds the scaled
  hardness limit, or is tagged Wither-immune.
- It is a portal, command/structure/jigsaw block, spawner, barrier, or another member
  of `#sololeveling:ability_destruction_immune`.
- Player interaction/use checks reject the position, `canEntityDestroy` rejects it,
  Forge's `BlockEvent.BreakEvent` is cancelled, or
  `ForgeEventFactory.onEntityDestroyBlock` rejects it.

Removal invokes the block's normal player-destruction lifecycle with drops disabled.
The synthetic break event is marked while posted so ability destruction cannot farm
the Daily Quest mining objective.

Claim/protection mods that cancel the standard Forge break event or interaction hooks
therefore retain authority over the mutation.

## Datapack extension point

The built-in tag is:

```text
#sololeveling:ability_destruction_immune
```

Its default entries protect progression walls and key blocks, dungeon spawners,
portals, instance infrastructure, evaluator blocks, the guild computer, and vanilla
operator/portal/unbreakable blocks. Modpack authors can add blocks or nested block
tags by supplying
`data/sololeveling/tags/block/ability_destruction_immune.json` with
`"replace": false`. This is the preferred extension mechanism; adding hardcoded
ability-specific block exclusions should be reserved for engine invariants.

## Rules for future abilities

Add destruction only when the attack has a committed environmental impact that a
player can predict. Select the smallest fitting shape, pass the same primary or
hybrid attribute used by its damage model, and mark only meaningful enhanced states
as empowered. Avoid terrain calls for heals, summons, transformations, defensive
constructs, movement-only skills, precision strikes, incidental projectiles, and
every tick of a long-lived area effect. Multi-stage attacks should normally submit
one final job or a small explicitly capped number of terrain pulses.
