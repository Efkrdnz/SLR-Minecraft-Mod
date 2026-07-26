# Monarch of Beasts — The Apex Hunt

Last updated: 13 July 2026  
Status: Detailed design and implementation plan. Every numeric value below is a first-playtest target, not final tuning.

[Open the combat-loop and implementation-roadmap board in FigJam](https://www.figma.com/board/zeodviJaMUQej9yq8zqBXT)

# Decision

Build Rakan, the Monarch of Beasts / Monarch of Fangs, as a single-target pursuit bruiser whose advantage comes from reading prey behavior and shaping routes.

The class does not win by filling a rage bar through ordinary damage. It wins by choosing one Quarry, reading delayed scent, creating a route with a voluntary claw-rift, funneling that route with temporary rubble, attacking from a genuinely new angle, then deciding whether to spend Hunt on a maul or regeneration or bank it for the full white-wolf body.

The launch kit is:

1. Law of the Hunt — Quarry, delayed scent, Hunt, Predator Feats, and Openings.
2. Rending Cadence — a three-beat enhanced basic claw chain.
3. Claw-Rift Passage — a visible, followable, momentum-preserving spatial route.
4. Rubble Jaw — a non-damaging V-shaped temporary terrain funnel.
5. King's Maul — an earned, flank-gated single-target payoff.
6. Feral Reconstitution — interruptible recovery of recent damage dealt by the Quarry.
7. Spiritual Body Manifestation: White Fang Sovereign — a short full-wolf ruleset that changes locomotion and skill verbs without becoming a flat-stat steroid.

No launch move is a radial damage pulse, generic ground slam, passive wallhack, summon army, auto-homing pounce, repeated teleport lock, or renamed version of an existing class skill.

# Canon anchors and original game design

## Canon anchors

Licensed Solo Leveling releases establish Rakan as a predator-first Monarch whose showcased identity is physical hunting, selective body transformation, a gigantic white-wolf spiritual body, rapid regeneration, telekinetic rubble control, and red claw-shaped spatial gates. Licensed story sources include [Tapas Solo Leveling](https://tapas.io/series/solo-leveling-comic/info), [Yen Press comic volume 11](https://yenpress.com/titles/9798400902550-solo-leveling-vol-11-comic), and [volume 12](https://yenpress.com/titles/9798400902574-solo-leveling-vol-12-comic). Exact scene-by-scene ability details are cross-checked against the chapter-referenced [Rakan reference](https://solo-leveling.fandom.com/wiki/Rakan), which is a secondary source.

Canon visual anchors:

- Wild black mane, fangs, yellow-to-red eyes, white or silver fur, asymmetric armor, and gold trim.
- Selective transformation of limbs and torso before the complete wolf body.
- Red, claw-shaped tears through space.
- Physical debris, dust, and oppressive red presence rather than elemental breath or a generic energy aura.
- A colossal white wolf as the complete spiritual body.

## Original gameplay translations

The following are deliberate original systems, not claims about named canon techniques:

- Quarry ownership and delayed scent footprints.
- Hunt as a spend-or-bank combat resource.
- Predator Feats and the two-Opening requirement for King's Maul.
- Rubble Jaw as a V-shaped route funnel.
- The exact MP, cooldown, damage, healing, PvP, and boss rules.
- True Body's item restrictions, movement handling, and altered ability rules.

The Fang of Rakan and Gray material belongs to Solo Leveling: Ragnarok. The licensed [Ragnarok chapter 10](https://tapas.io/episode/3030141) supports an optional future Fang branch, but it is out of the launch scope and must not be mixed into the original-series class fantasy without a clear label.

# Fit inside this project

## Identity and job slot

- Implement this as JOB 9 with vessel identity rakan.
- JOB 1–6 are already occupied; JOB 7 and 8 are reserved for Sung Il-Hwan and Go Gunhee.
- It is a vessel/job moveset, not a seventh base hunter class.
- The central cast path should remain JobSkillManager rather than expanding the legacy procedure chain.
- Exact skill strings must be declared once and reused by the dispatcher, tooltips, equipped-skill UI, HUD icons, cooldowns, localization, and tests.

## Existing-kit collision check

| Existing kit | Occupied design space | Beast rule |
| --- | --- | --- |
| Shadow Monarch | Summon inventory, army command, exchange, formations | No army, pet roster, or shadow-style teleport |
| White Flame | Breath, brand-domain loop, spear, doppelganger dodge, royal guard summons | No breath, brand field, clone interception, or summons |
| Frost | Anchor/recall spear, catch-release, temporary traversal path, recorded movement, moving cover | No tether, rewind, capture-release, path bridge, or projectile wall |
| Goliath | Capture/throw, formation smash, radial collapse, guided pursuit | No grab, throw, slam, radial lock, or guided target dash |
| Liu Zhigang | Counter, charged beam, target dance, sword domain, echo marks | No parry/counter, beam, chain teleport, or echo domain |
| Legacy classes | Backstab, Shadowstep, Detection, Leap, Taunt, Ground Slam, traps | No renamed legacy move |

The class occupies the remaining high-value space: prey-behavior reading, delayed information, voluntary route creation, asymmetric escape denial, earned flank pressure, and conditional regeneration.

# Class profile

## Combat role

Positional pursuit bruiser / anti-kite duelist.

## Strengths

- Excellent pressure on one important target.
- Strong use of corners, block faces, elevation, and escape lanes.
- Can create team-usable traversal without becoming a safe teleport.
- Earned boss interaction through tagged interrupt windows rather than hard crowd control.
- High mastery ceiling from angle changes, timing, and resource choices.

## Weaknesses

- Hunt is tied to one Quarry and decays when the chase collapses.
- Multi-attacker pressure directly counters Feral Reconstitution.
- Neutral front-facing damage should be below a sword specialist or Goliath.
- Claw-Rifts are visible, armed with delay, destructible, and followable.
- Rubble Jaw can be jumped, broken, or routed around.
- True Body removes shield, bow, block placement, and normal item-use flexibility.
- No passive damage reduction, armor penetration, true damage, or free healing.

# Core system — Law of the Hunt

## Quarry

Only one eligible hostile can be the Quarry.

Claim rules:

- The first successful Seeking Rake claims a Quarry when none exists.
- Sneak + a successful first Cadence hit requests a manual switch.
- Switching away from a living, engaged Quarry costs 15 Hunt and applies a four-second swap lock.
- Switching is free when the prior Quarry is dead, unloaded, invalid, more than 24 blocks away without line of sight for four seconds, or out of combat for ten seconds.
- The target receives a clear fang icon and a one-time audio warning.
- A Quarry can be a normal mob, elite, boss, or PvP opponent.
- Reject self, allies, party members, creative/spectator players, owned pets, owned summons, shadows, armor stands, training dummies, and targets the player cannot harm.

Clear Quarry on death, logout, dimension change, job loss, invalid ownership, or server cleanup.

## Delayed scent

Scent shows where the Quarry was, not where it is through walls.

- Sample the Quarry's actual server position and horizontal direction every four ticks.
- Display up to three seconds of history with an approximately 0.8-second delay.
- Footprints and scent ribbons are depth-tested and do not render through solid blocks.
- No glowing outline, live skeleton, nameplate reveal, minimap marker, or forced camera tracking.
- Invisibility reduces decorative scent density but does not erase the essential last-known footprint while the target is otherwise eligible.
- Low graphics keeps four to six footprint stamps. Higher settings add a thin owner-only ribbon and direction ticks.
- Server logic independently validates Feats; client scent visuals never authorize damage or Hunt.

## Hunt meter

Hunt ranges from 0 to 100 and is shown as four ivory fang segments, each worth 25.

Hunt is earned from Predator Feats, not ordinary damage:

| Predator Feat | First-test gain | Server condition |
| --- | --- | --- |
| Pursuit | 10 | Hit after reducing the gap by at least six blocks while the Quarry moved at least four |
| Angle Break | 12 | Complete Cadence from a relative attack angle at least 60 degrees different from the prior confirmed angle |
| Rift Ambush | 16 | Hit within 24 ticks of personally crossing a valid Claw-Rift, after at least five blocks of displacement and a 60-degree angle change |
| Herd | 16 | Hit within 40 ticks after the Quarry exits Rubble Jaw's mouth or changes heading by at least 75 degrees near it |
| Cull | 20 | Defeat an eligible Quarry with valid player kill credit |

Anti-farm rules:

- Each Feat has a per-Quarry cooldown of six seconds.
- The same Feat cannot count twice consecutively.
- Maximum Hunt gain is 18 per second.
- One cast ID can award each relevant Feat only once, including delayed phases.
- Damage to allies, pets, summons, training targets, or self-owned entities never counts.
- Cull cannot be repeatedly farmed from the same respawned or summoned owner source.
- No Hunt gain during True Body, Sated, or exhaustion unless a form rule explicitly extends duration from a new distinct Feat.

Decay rules:

- Decay begins five seconds after the last valid hunt interaction.
- Lose four Hunt per second while the Quarry is invalid, unreachable, or the hunter disengages.
- Clear Hunt on death, logout, dimension change, job loss, or successful True Body activation.

## Openings

King's Maul requires two different Predator Feats against the same Quarry within eight seconds.

- Show the two confirmed Opening icons near the Hunt meter.
- The Quarry receives a small rear-fang warning when the second Opening is confirmed.
- Repeating the same Feat never supplies the second Opening.
- A successful Maul consumes both Openings.
- Openings expire independently of Hunt.
- A failed pre-validation consumes nothing. A committed Maul that the target dodges is a real whiff and pays its cost.

This makes the finisher an earned behavior read rather than a low-health execute button.

# First-playtest balance sheet

Reference baseline: level 60, Strength 60, Speed 60, Perception 60, Vitality 60, Intelligence 60, roughly 7,000 maximum MP, roughly 50 health, and roughly 15 armor.

Use VesselManaScaling for the physical skill costs so high Strength does not make abilities effectively free.

| Action | MP | Hunt | Cooldown / lock | Primary role |
| --- | --- | --- | --- | --- |
| Cadence beat 1 / 2 / 3 | 40 / 50 / 70 | 0 | 5 / 5 / 7 ticks | Narrow sustained pressure |
| Claw-Rift Passage | 260 | 0 | 11s | Visible route and angle creation |
| Rubble Jaw | 380 | 0 | 14s | Temporary escape-lane funnel |
| King's Maul | 480 | 25 | 12s | Earned single-target payoff |
| Feral Reconstitution | 360 split payment | 30 on success | 18s success; 8s interrupt | Recent-wound recovery |
| White Fang Sovereign | 900 split payment | 100 on success | 35s exhaustion | Short rule-changing form |

Every successful cast applies the normal mana-refresh lock. Failed server validation does not consume MP, Hunt, cooldown, or form state.

# Enhanced basic — Rending Cadence

## Purpose

Rending Cadence is the reliable pressure tool and Quarry claim. It rewards real angle changes without becoming Liu's dash chain or Goliath's radial combo.

## Flow

1. Seeking Rake — narrow diagonal claw capsule with a small collision-safe forward step. Claims a Quarry if none exists.
2. Orbiting Claw — A or D chooses a short side step around the target. It is swept and canceled by collision; it never passes through entities or blocks.
3. Cross-Rend — precise crossed claws. It grants Angle Break only when the server confirms a meaningful relative-angle change.

The combo resets after 18 ticks without the next beat, on shielded interruption, invalid target, job loss, or movement-state cleanup.

## Damage

Define the level-scaled baseline:

D = 8 + Strength / 12

Beat ratios:

- Seeking Rake: 0.65 × D
- Orbiting Claw: 0.75 × D
- Cross-Rend: 1.00 × D

At Strength 60, the complete raw chain is approximately 31.2 before armor. Apply a 0.78 PvP multiplier to the chain. Respect armor, invulnerability rules, damage events, party protection, and normal mitigation. Do not add armor penetration or true damage.

Only the first collision-confirmed target receives full damage. The launch implementation should not add incidental radial cleave.

## Counterplay

- Maintain a stable facing and deny the 60-degree angle change.
- Use a shield or terrain to break the cadence.
- Step into a narrow doorway where the side step cannot validate.
- Force the hunter to switch targets and lose Hunt.
- Pressure during the seven-tick finisher recovery.

## Visual direction

Each beat selectively manifests forearms and claws:

- Opaque/cutout ivory fur and claw silhouette on the player layer.
- One tapered crimson claw ribbon per hand, not a particle cylinder.
- Direction ticks on the Quarry's feet communicate the previous confirmed attack angle.
- Angle Break creates a brief three-slit crown mark and a dry bone-snap cue.
- Low graphics keeps the slash shape and angle tick with no fur smoke.

# Skill 1 — Claw-Rift Passage

## Purpose

A visible spatial route that preserves movement and can be followed. It is not Shadow Exchange, an instant target teleport, or a safe escape.

## Controls and flow

- One press places an Entry approximately one block in front of the caster and an Exit on the valid block face under the crosshair, up to 14 blocks away.
- The Exit orientation follows the selected surface normal.
- Both planes appear immediately but arm after eight ticks.
- The pair remains for four seconds.
- A living entity crosses only by moving through the front of the Entry plane after arming.
- The entity exits just outside the Exit plane with its horizontal momentum rotated into the Exit orientation.
- Preserve momentum magnitude, capped at 1.6 blocks per tick. Do not erase existing fall-distance debt.
- One entity can use a pair once. Apply a ten-tick rift immunity to prevent loops.
- Base form is one-way. No projectile transport in the first implementation.
- Maximum one pair per caster. A new cast closes the old pair.

Eligible living entities may voluntarily use the route. Enemies can follow the hunter, which is intentional counterplay. Bosses, vehicles, mounted stacks, passengers, and entities too large for the validated exit volume cannot cross in the first implementation. Never place an Entry overlapping an entity bounding box.

## Validation and safety

Reject the cast if either plane would:

- intersect occupied collision;
- lead into lava, void, fluid, world-border violation, or an unloaded chunk;
- overlap a portal, block entity, protected claim, or unsafe destination;
- produce no collision-safe exit volume;
- exceed the configured distance.

Server authority owns crossing, position, velocity rotation, MP, cooldown, and Feat confirmation. Never trust a client-declared destination.

## Counterplay

- Read the eight-tick arming tell.
- Follow through the same route.
- Strike either seam to remove one of its three integrity points.
- Move away from the predicted Exit.
- Block the Entry approach.
- Force the hunter to use the route defensively and lose the Rift Ambush opportunity.

## Predator Feat

Rift Ambush requires:

- caster personally crosses their pair;
- Entry-to-Exit displacement is at least five blocks;
- next confirmed Cadence hit occurs within 24 ticks;
- relative attack angle changes by at least 60 degrees;
- same cast has not already awarded the Feat.

The rift itself deals no damage and never awards Hunt.

## Visual and shader direction

- Three red claw slits on the surface with a dark parallax interior.
- Depth-tested crimson rim, black core, and sparse inward particles.
- UV noise bends the interior and creates fake refraction; do not require framebuffer refraction.
- Entry streaks flow inward; Exit streaks flow outward.
- A thin ground arrow shows exit direction for all nearby players.
- Shader-off fallback uses three animated translucent quads plus vanilla reverse-portal particles.
- No full-screen effect for remote viewers.

## True Body rule — Two-Way Hunt

In White Fang Sovereign, the same pair is two-way for one use in each direction per entity.

- It is still visible, armed, destructible, finite, and followable.
- No extra range, damage, invulnerability, or homing is added.
- The meaningful change is route planning: the wolf may commit through, circle physically, and use the remaining direction to return.

# Skill 2 — Rubble Jaw

## Purpose

Telekinetically raise a short V-shaped obstacle that makes the Quarry choose a route. It deals no damage and does not root, pull, slow, or close around a target.

## Controls and geometry

- Target a valid ground point within 12 blocks.
- The jaw faces the caster at commit time.
- After a six-tick lift telegraph, raise two angled arms with a two-block mouth.
- Maximum eight temporary collision blocks: low inner fangs and taller outer tips.
- Duration: five seconds.
- Maximum one jaw per caster.
- Each segment is breakable in approximately two ordinary hits, has no drops, cannot be piston-moved, and cannot become a permanent building block.
- Players can jump the low inner fangs, break a side, pass through the mouth, go around, or use a mobility skill.
- The jaw does not move after placement.

The visual fiction is telekinetic rubble. Do not delete or snapshot real terrain. Use dedicated temporary marker blocks and opaque rubble visuals.

## Placement safety

Place only into air and never into:

- living-entity bounding boxes;
- fluids;
- block entities;
- unloaded chunks;
- protected claims;
- world-border exterior;
- portals or indestructible structures;
- another caster's temporary marker.

Cleanup removes a segment only if the block is still the exact temporary marker. Add a scheduled self-expiry so a server restart cannot leave permanent rubble. Never restore an old terrain snapshot over later player construction.

## Predator Feat

Herd becomes eligible when the Quarry:

- passes through the jaw mouth; or
- changes horizontal heading by at least 75 degrees within three blocks of a jaw segment.

The caster must then land a Cadence hit within 40 ticks. Jumping or breaking through remains valid counterplay; breaking a segment alone does not give the hunter a free Opening.

Bosses are never trapped or displaced. If a large boss legitimately crosses the mouth geometry, it may enable Herd; otherwise the skill is allowed to be matchup-dependent.

## Counterplay

- Jump a low fang.
- Break a segment.
- Move around the finite arms.
- Occupy the intended placement before the six-tick lift completes.
- Use a route the caster did not anticipate.
- Pressure the caster instead of accepting the funnel.

## Visual and shader direction

- Opaque block-model rubble provides readable collision and correct depth.
- Crimson telekinetic filaments use a depth-tested additive shader.
- Stone shards rise along curved paths into each fang; do not spawn an entity per block.
- The mouth has two subtle ground chevrons so both sides understand the intended route.
- The final 20 ticks pulse and shed dust to telegraph expiry.
- Low graphics keeps the opaque rubble, mouth chevrons, and expiry crack.

## True Body rule — Sovereign Through His Den

The transformed caster can pass through their own Rubble Jaw segments.

- Each crossed segment visibly dissolves around the wolf and reforms behind it.
- Other entities still jump, break, or route around normally.
- The jaw does not close, damage, or slow.
- The change creates asymmetric flanking without turning the skill into a damaging cage.

# Skill 3 — King's Maul

## Purpose

An earned single-target finisher that converts a correct flank and prey state into pressure. It is not an automatic execute, target teleport, grab, or cinematic stun.

## Requirements

- Same Quarry has supplied two different Openings within eight seconds.
- Caster has at least 25 Hunt and required MP.
- Caster is within 4.5 blocks, has line of sight, and stands inside the current open-flank wedge.

Compute the open flank from the Quarry's averaged horizontal movement over the previous 12 ticks. If the target is nearly stationary, use the opposite of its current look direction. The wedge has a first-test half-angle of approximately 50 degrees.

Both players see a compact fang marker for the valid rear wedge. It must not rely on color alone.

## Cast

- Six-tick forelimb and jaw coil.
- Snapshot the attack line at commit; no homing after commit.
- Perform a maximum 2.5-block collision-swept step.
- Strike the first valid Quarry collision only.
- A target that moves or turns can cause a real miss.
- Success consumes both Openings, 25 Hunt, MP, and full cooldown.
- A committed whiff still consumes its committed cost. Pre-validation failure consumes nothing.

## Damage

18 + Strength / 6 + Perception / 20

At Strength 60 and Perception 60, raw damage is 31 before armor. Apply a 0.65 PvP multiplier. Respect armor and normal mitigation. One cast has one direct hit and no radial aftershock.

## Behavior conversion

Resolve one contextual effect, never all of them:

- Fleeing or sprinting: multiply current horizontal velocity once by 0.70 PvE or 0.85 PvP; cancel sprint for six PvE ticks or three PvP ticks.
- Blocking: apply double normal shield/guard durability pressure; do not force a long shield-disable cooldown.
- Supported interruptible channel: request one interrupt. Players gain four seconds and bosses ten seconds of repeat-interrupt immunity.
- Otherwise: damage only.

Knockback-immune bosses are never moved. Untagged boss attacks are never canceled. The first implementation must not add percentage-health damage.

## Counterplay

- Prevent two different Feats.
- Face the hunter and rotate the flank wedge.
- Back the valid wedge into terrain.
- Step out during the six-tick coil.
- Break line of sight.
- Have an ally pressure the hunter.
- Use an uninterruptible boss phase or wait out the Openings.

## Visual and shader direction

- Selectively manifest jaw, shoulders, and foreclaws.
- The open wedge uses two ivory ground fangs and a red direction notch.
- The windup compresses fur and pulls nearby dust inward.
- On a successful state conversion, use a distinct cue: skid streak, guard sparks, or broken channel glyph.
- No screen shake above the user's configured combat-shake limit.
- Low graphics always preserves the wedge and coil silhouette.

## True Body rule — Momentum Maul

The targeted swept step is removed.

- The wolf must physically enter the valid wedge while sprinting.
- Activating Maul converts current momentum into a bite-and-vault past the Quarry if the exit is collision-safe.
- Damage and control caps remain unchanged.
- There is no homing, carry, grab, or target lock.
- The rule change rewards actual quadruped route handling rather than making the normal Maul easier.

# Skill 4 — Feral Reconstitution

## Purpose

Recover part of recent damage inflicted by the current Quarry through an interruptible regrowth channel. This is not passive regeneration or a generic heal.

## Wound ledger

Record post-mitigation health loss caused by the current Quarry during the previous four seconds.

Do not record:

- damage from other attackers;
- environmental, void, starvation, or self damage;
- absorption loss;
- damage prevented by invulnerability;
- fatal damage after death;
- damage from invalid allies or ownership sources.

Fire and explicit anti-heal effects mark half of the recorded amount as unrecoverable.

## Activation and payment

Requirements:

- at least one recoverable wound from the current Quarry;
- 30 Hunt available;
- skill ready and caster alive.

Payment is split:

1. Pay 180 MP to begin the 24-tick channel.
2. On successful completion, pay the remaining 180 MP and 30 Hunt.
3. If interrupted, lose the initial payment, apply an eight-second partial lock, and keep Hunt.
4. Successful completion applies the full 18-second cooldown.

During channel:

- movement is limited to approximately 60%;
- attacks, item use, Rubble Jaw, and Claw-Rift are disabled;
- any new direct health damage interrupts;
- no invulnerability, knockback immunity, or crowd-control immunity is granted.

## Healing

PvE:

min(45% of eligible recorded damage, 6 + Vitality / 10, 25% of maximum health)

At Vitality 60 and approximately 50 maximum health, the practical cap is about 12 health.

PvP:

min(30% of eligible recorded damage, 6 health, 12% of maximum health)

Never overheal and never restore a dead player.

## Counterplay

- Any attacker can interrupt the channel.
- Damage from non-Quarry attackers cannot be recovered.
- Fire and anti-heal reduce the ledger.
- Force the hunter to spend Hunt defensively and delay True Body.
- Break line of sight only after applying pressure; the hunter still needs a safe channel window.

## Visual and shader direction

- Identify the recent impact side and grow opaque/cutout white fur inward across that limb or torso region.
- Add short crimson cracks that seal from the edges inward.
- Use sparse inward-flowing motes; no constant heal halo.
- The channel has an unmistakable audio cadence and visible break state.
- Low graphics uses a white-fur overlay, three inward particles, and the same audio cue.

## True Body rule — Shed the Mortal Shell

Casting Reconstitution during White Fang Sovereign immediately commits to ending the form.

- Begin a shorter 12-tick visible shedding channel.
- The old wolf silhouette peels into a stationary fur husk while the player returns to humanoid form.
- Healing amount and caps do not increase.
- Interruption loses the form and prevents healing.
- This creates a real survival-versus-offense decision instead of passive ultimate regeneration.

# Spiritual Body Manifestation — White Fang Sovereign

## Activation

Requirements:

- 100 Hunt;
- 900 MP;
- no active Sated or manifestation exhaustion;
- no conflicting monarch form.

Activation is a 12-tick visible channel with no invulnerability.

Split payment:

- 180 MP on channel start.
- Remaining 720 MP and all 100 Hunt on successful completion.
- An interrupted channel loses only the initial MP and applies a six-second retry lock.
- Successful activation suppresses MP regeneration for the form.

## Duration and exit

- Base duration: 14 seconds.
- Each different Predator Feat completed during the form adds one second, maximum four added seconds.
- Repeating one Feat does not extend duration.
- Hard cap: 18 seconds.
- Manual Reconstitution, death, logout, dimension change, job loss, or conflicting form ends it.
- After exit: six seconds Sated with no Hunt gain and 35 seconds manifestation exhaustion.

## Form rules

This is a locomotion and sequencing form, not a stat steroid.

- No passive damage reduction.
- No armor, reach, attack-speed, or generic damage bonus.
- No passive regeneration.
- No cooldown reduction.
- No shield, bow, block placement, or normal item use.
- Maximum horizontal speed remains governed by the player's existing movement stat.
- Gain one-block auto-step, better preservation of sprint over small terrain changes, and a wider turning radius at full speed.
- Water, webs, slows, knockback, collision, and low ceilings remain relevant.
- Keep the normal gameplay collision box. The production visual must stay close enough to it for PvP readability.
- A large spectral wolf apparition may appear during activation only; the persistent combat body must clearly show the hittable core.

## Altered moves

- Rending Cadence becomes four beats distributed across foreclaw, foreclaw, bite, and collision-safe vault. Total damage budget stays approximately equal to the normal three-beat chain.
- Claw-Rift becomes two-way for one use in each direction per entity.
- Rubble Jaw becomes permeable only to its transformed owner.
- King's Maul becomes a momentum-gated bite-and-vault with the same damage and control caps.
- Feral Reconstitution ends the form and starts Shed the Mortal Shell.

The form changes decisions and movement. It must not quietly reintroduce generic damage inflation through extra hits.

## Counterplay

- Interrupt the activation channel.
- Deny varied Feats so the form cannot extend.
- Fight from multiple angles; Reconstitution only records Quarry damage.
- Follow or destroy the rift.
- Use water, webs, tight doors, elevation, and sharp turns against the wolf's turning radius.
- Kite the momentum-gated Maul rather than standing in its flank wedge.
- Force an early Shed the Mortal Shell.

# Spiritual-body visual specification

## Silhouette

Humanoid selective manifestations:

- forearm fur, long claws, altered jaw line, red eyes, and a broken gold-trim shoulder plate;
- transformation remains local to the body part used by each move;
- no constant giant aura obscuring the player.

White Fang Sovereign:

- custom articulated white-wolf model;
- black mane ridge, bone-white claws, red eyes, sparse gold armor remnants;
- solid readable core with thin emissive accents;
- activation-only colossal spectral wolf behind the player;
- production combat silhouette kept close to the player collision volume.

The existing Elder Beast model is acceptable only as a grey-box proxy. Its current tameable AI and summon logic are not suitable for this class.

## Render architecture

Use two persistent body passes:

1. Opaque/cutout fur and armor base with normal lighting, depth writes, and shadows.
2. Depth-tested additive eye, claw-edge, and crack mask with color-only writes.

Do not make the full articulated wolf translucent. It will self-sort poorly around water, glass, shader packs, and its own limbs.

Use DeferredWorldShaderRenderer.buffer for custom world quads and keep Iris/Oculus shadow-pass exclusions. Cache RenderTypes; never create a new RenderType per draw.

## Beast VFX shader

Create one shared Beast VFX shader with style encoded in stable geometry data and at least three cached render states:

- Surface: translucent, depth-tested, no depth write, double-sided where needed.
- Additive: depth-tested additive, color-only.
- Distortion accent: limited depth-aware UV distortion, disabled when incompatible.

Suggested style IDs:

1. Claw ribbon.
2. Rift interior.
3. Scent footprint and ribbon.
4. Telekinetic filament.
5. Regrowth crack.
6. Manifestation burst.

Stable inputs:

- Integer part of U: style.
- Fractional U and V: local effect coordinates.
- Vertex RGB: tint.
- Vertex alpha: fade and intensity.
- GameTime: animation.
- Synced entity data or packet payload: seed, lifetime, direction, scale.

Use one lightweight BeastVfxEntity per active cast at most. Client-rendered sub-effects should be batched and capped. Never use an entity per particle, footprint, rubble segment, or trail sample.

## Post-processing

Use a brief caster-only activation pass:

- six ticks on medium;
- six to eight ticks on high;
- off on low or reduced-motion;
- one full-screen pass with approximately four texture samples per pixel;
- red pupil contraction, edge desaturation, and three-claw spatial warp;
- overlay fallback when compilation or shader-pack compatibility fails.

No continuous full-screen effect during True Body. Coordinate with the existing Liu impact post-process through a shared screen-impact owner so chains cannot conflict.

## VFX quality budget

| Feature | Low | Medium | High |
| --- | --- | --- | --- |
| Scent stamps | 6 | 12 | 18 |
| Claw ribbon segments | 6 | 12 | 20 |
| Rift layers | 1 + core | 2 + core | 2 + distortion |
| Rubble decorative shards | 4 | 8 | 12 |
| Fur shells | 1 | 2 | 2 |
| Aura / molt puffs | 8 | 20 | 32 |
| Activation post | Off | 6 ticks | 6–8 ticks |

Distance behavior:

- 0–16 blocks: selected quality.
- 16–32: approximately 60% decorative density.
- 32–48: essential telegraphs and body silhouette.
- Beyond 48: minimal gameplay-readable rift, rubble, Maul, and form cues only.

Always preserve:

- rift plane and exit arrow;
- Rubble Jaw collision silhouette and expiry;
- Openings and Maul wedge;
- Reconstitution channel;
- True Body activation and hittable core.

Add VFX density, combat shake, reduced-motion, and owner vignette controls. Pulse effects below 3 Hz and never communicate a state with color alone.

# Sound direction

Register original events and subtitles for:

- Quarry claim.
- Scent confirmation.
- Claw beat 1 / 2 / 3.
- Rift carve, arm, cross, integrity hit, and collapse.
- Rubble lift, settle, break, and expiry.
- Opening confirmed.
- Maul windup, whiff, and each contextual conversion.
- Reconstitution channel, completion, and interruption.
- True Body channel, activation, extension, shed, and exit.

Existing generic slash/dash/impact audio is acceptable for grey-box work only.

# Server implementation map

## Primary manager

Add net.solocraft.util.BeastMonarchManager patterned after the modern vessel managers.

Responsibilities:

- JOB 9 and rakan identity validation.
- Quarry UUID, switch cost, engagement expiry, and eligibility.
- Hunt, Feat cooldowns, last Feat, Openings, decay, and anti-farm intervals.
- Cadence step, prior attack vector, timing, and swept hit checks.
- Active rift pair, integrity, traversal immunity, and cleanup.
- Rubble Jaw cast ID, block positions, mouth geometry, Herd state, and cleanup.
- Maul wedge, snapshot, behavior conversion, and immunity tags.
- Wound ledger and Reconstitution channel.
- True Body channel, form state, duration, Feat extensions, Sated, and exhaustion.
- Cleanup on death, clone/respawn, logout, dimension change, and job loss.
- Compact synchronization only when state changes.

Use UUID ownership everywhere.

## State records

Suggested transient state:

- QuarryState: target UUID, claimed tick, last interaction, last line-of-sight, recent samples.
- HuntState: value, last gain, last Feat, per-Feat timestamps, Opening mask and expiries.
- CadenceState: beat, expiry, previous confirmed relative vector.
- RiftState: dimension, Entry plane, Exit plane, armed tick, expiry, integrity, used entity UUIDs.
- RubbleState: dimension, cast UUID, positions, mouth plane, expiry.
- MaulState: target UUID, snapshot position, attack vector, resolve tick.
- WoundState: source UUID, post-mitigation amount, timestamps, fire/anti-heal fraction.
- FormState: channel, active expiry, unique extension mask, Sated/exhaustion.

Do not full-sync these records every tick.

## Job and UI wiring

Update:

- util/JobSkillManager.java
- util/SkillListHelper.java
- util/VesselManager.java
- procedures/UseSkillOnKeyPressedProcedure.java
- procedures/ReturnJobProcedure.java
- procedures/TitleTextProcedure.java
- client/screens/DisplayOverlay.java
- client/gui/EquippedAbilitiesScreen.java
- command/SlrCommand.java
- init registries for blocks, entities, renderers, sounds, and packets
- assets/sololeveling/lang/en_us.json
- exact skill icons under assets/sololeveling/textures/screens

If any Beast action needs hold/release later, add explicit packet routing. The launch design intentionally uses press-based activation so it does not depend on the current non-generic hold/release path.

## Combat and safety rules

- Use CombatRangeHelper for large hitboxes and boss surface distance.
- Use the modern validTarget policy before damage, movement, or Feat gain.
- Use player-attributed physical damage so kill credit remains correct.
- Respect armor and normal mitigation.
- Never repeatedly set player positions to simulate a root.
- Movement uses swept AABB and collision-safe destinations.
- Boss interruption requires an explicit interruptible tag.
- PvP re-interrupt immunity: four seconds.
- Boss re-interrupt immunity: ten seconds.
- No forced chunk loading.
- No wide per-tick entity scans; throttle to every two to four ticks and cap candidates.
- No command particles.

# Balance tracking framework

## Success metrics

| Metric | Target |
| --- | --- |
| Neutral dummy DPS | 10–15% below a sword specialist / best melee vessel |
| Skilled hunt DPS | No more than about 5% above comparable melee after earned Feats |
| First True Body | 35–50 seconds of active, successful hunting |
| Maul availability | Roughly once per 6–10 seconds when playing well |
| Maul hit rate | 35–55% |
| Rift successful traversal | At least 70% after usability tuning |
| Rift Ambush conversion | 25–40% |
| Rubble route change | 30–50% of valid casts |
| Reconstitution completion | 45–65% PvE; 25–45% PvP |
| Effective Reconstitution healing | Below 15% of incoming damage over a ten-second pressure window |
| PvP forced-control chain | Below 0.35 seconds |
| True Body damage uplift | 0–10%; mobility and rule changes carry the form |
| True Body TTK uplift | No more than 15% |
| Nearby VFX | No missed gameplay frames or uncontrolled batch growth |

## Test matrix

Run at levels 30, 60, 100, and high-stat endgame.

Scenarios:

- stationary dummy;
- mobile melee mob;
- ranged mob pack;
- solo dungeon;
- co-op boss;
- knockback-immune boss;
- interruptible and uninterruptible boss phases;
- melee 1v1;
- ranged 1v1;
- shield 1v1;
- 3v3 focus pressure;
- 0, 100, and 200 ms latency;
- low ceilings, doors, stairs, slabs, water, webs, ravines, and world border;
- protected claims and block-place denial;
- death, respawn clone, logout/login, dimension change, job change;
- multiple simultaneous Rakan players;
- vanilla graphics, low VFX, shader-off fallback, Oculus/Iris, and shader compilation failure.

## Instrumentation

Add a server-debug switch that records aggregate counters, never chat spam:

- time to claim Quarry;
- Hunt gained by Feat;
- denied duplicate/anti-farm gains;
- Hunt spent on Maul, Reconstitution, and True Body;
- Maul validation, commit, hit, whiff, and contextual outcome;
- Rift invalid-placement reasons, crossings, follows, and safety cancels;
- Rubble blocks placed, blocked, broken, expired, and orphan-cleaned;
- recorded versus restored wound amount;
- True Body activation interruption, duration, unique extensions, and exit reason;
- VFX instance count, captured batches, and peak vertices.

## Tuning order

1. Fix invalid or confusing mechanics.
2. Tune Hunt gain, decay, Openings, and spend decisions.
3. Tune telegraph, recovery, duration, and cooldown.
4. Tune MP.
5. Tune damage and healing last.
6. Never solve a balance problem by turning a unique move into a generic damage field.

# Delivery roadmap

Estimated solo-development range: six to eight focused weeks, depending on animation and model production. Visual work can overlap the gameplay phases after the grey-box loop is approved.

## Phase 0 — Combat contract and harness (1–2 days)

- Freeze exact names, Quarry rules, Feats, PvP caps, boss tags, and no-wall-vision policy.
- Add config-backed first-test constants.
- Add a debug command and metrics sink.
- Define cleanup invariants and test fixtures.
- Gate: no implementation starts while two abilities still solve the same problem.

## Phase 1 — JOB 9 and state foundation (2–3 days)

- Add rakan vessel identity and JOB 9.
- Wire JobSkillManager, SkillListHelper, command assignment, titles, localization, and placeholder icons.
- Add BeastMonarchManager lifecycle and compact sync.
- Add placeholder Hunt/Openings HUD.
- Test death, logout, clone, dimension, and job removal.

## Phase 2 — Grey-box hunt loop (4–5 days)

- Implement Quarry claim/switch/expiry.
- Implement delayed scent sampling and a no-shader footprint fallback.
- Implement Hunt gain/decay, anti-farm, and Openings.
- Implement Rending Cadence with swept hit checks and angle math.
- Compare neutral DPS to Goliath and Liu.
- Gate: five-minute combat test must be fun with placeholder particles before building the ultimate.

## Phase 3 — Claw-Rift Passage (3–4 days)

- Implement safe plane placement and validation.
- Implement arming, one-use crossing, momentum rotation, integrity, expiry, and cleanup.
- Implement Rift Ambush validation.
- Test followers, rapid re-entry, falling, water, low ceilings, unloaded chunks, claims, and latency.
- Gate: zero suffocation/void cases across the traversal test suite.

## Phase 4 — Rubble Jaw (3–4 days)

- Add temporary RubbleFangBlock with scheduled self-expiry.
- Add V-geometry solver, air-only placement, protected-world checks, break behavior, and exact cleanup.
- Implement mouth crossing, turn detection, and Herd.
- Test simultaneous casters and server restart cleanup.
- Gate: no permanent blocks and no overwritten player construction.

## Phase 5 — Hunt spenders (4–5 days)

- Implement King's Maul wedge, commit snapshot, whiff, damage, behavior conversion, and immunity tags.
- Implement Quarry-only wound ledger.
- Implement split-payment Reconstitution, interruption, caps, and fire/anti-heal handling.
- Test 1v1, focus fire, bosses, shields, and channel interruption.
- Gate: PvP control below 0.35 seconds and recovery below the healing budget.

## Phase 6 — White Fang Sovereign (5–7 days)

- Implement activation channel, split payment, form state, Sated/exhaustion, and cleanup.
- Implement four-beat equal-budget Cadence.
- Implement two-way rift, owner-only Jaw phasing, Momentum Maul, and Shed the Mortal Shell.
- Add item-use restrictions and movement handling.
- Use the Elder Beast geometry only as a temporary proxy.
- Gate: form is enjoyable without any flat damage or armor bonus.

## Phase 7 — Production presentation (5–7 days, partly parallel)

- Build custom Rakan humanoid manifestation parts and white-wolf model/animations.
- Add BeastVfxEntity, cached RenderTypes, shared shader, and fallbacks.
- Add scent, claw, rift, telekinesis, regrowth, and form visuals.
- Add brief activation post-process and shared post owner.
- Add sounds, subtitles, final icons, tooltips, HUD, reduced-motion, VFX density, and shake controls.
- Test slim/default player models, first person, invisibility, water, glass, and shader packs.

## Phase 8 — Balance, performance, and release hardening (5–7 days)

- Run the complete level, matchup, latency, lifecycle, and graphics matrix.
- Tune Hunt and windows before damage.
- Profile with Spark during multiple Rakan players and overlapping VFX.
- Confirm no persistent AI entities, command particles, wide scans, or RenderType churn.
- Validate late join, packet ordering, client fallback, and server-authoritative outcomes.
- Gate: all acceptance criteria and release metrics pass.

# QA checklist

## Gameplay

- Every button solves a different problem.
- Quarry never produces a live through-wall outline.
- The same Feat cannot farm Hunt consecutively.
- Maul requires two different Openings and a real flank.
- Rift is visible, followable, finite, and never damages by itself.
- Rift never erases fall debt or allows a loop.
- Rubble Jaw deals no damage and never closes around a target.
- Reconstitution restores only eligible recent Quarry damage.
- Any direct hit interrupts Reconstitution.
- True Body does not add passive armor, damage reduction, or regeneration.
- No move becomes a radial damage pulse in manifested form.

## PvP

- canHarmPlayer and party protection are checked before damage and Feat gain.
- No forced camera, aim, input lock, deselection, repeated setPos, or hard root.
- Maul movement reduction is one-time and capped.
- Interrupt immunity is enforced.
- Rift placement cannot overlap a player and has an eight-tick arm.
- The visual wolf clearly communicates the normal collision core.
- Multi-attacker pressure counters the wound ledger.

## Bosses

- No boss teleport, drag, root, or percentage-health damage.
- Only explicitly interruptible actions can be canceled.
- Boss surface distance uses hitbox-aware range.
- Rubble Jaw may be matchup-dependent and must not fake usefulness with free boss damage.
- Uninterruptible phases remain uninterruptible.

## World and lifecycle

- Temporary rubble removes only its exact marker.
- Scheduled expiry handles restart recovery.
- No snapshots overwrite later construction.
- No forced chunks.
- Death, clone, logout, dimension, job loss, and conflicts clear all states.
- Late-joining clients receive correct form, Quarry warning, rift, and rubble state.

## Rendering and accessibility

- Essential telegraphs work with the custom shader disabled.
- Opaque/cutout bodies write depth correctly near water and glass.
- Iris/Oculus shadow passes do not duplicate effects.
- No continuous full-screen post.
- Low graphics preserves rift, rubble, Maul, Reconstitution, and form tells.
- Reduced-motion removes nonessential distortion and afterimages.
- Color is always paired with shape and audio.
- VFX density drops decoration before telegraphs.

# Acceptance criteria

The class is ready for implementation when:

- Rakan is mapped to JOB 9 and the modern job-skill pipeline.
- Each move has a unique gameplay verb and explicit counterplay.
- Canon anchors and original inventions are clearly separated.
- The complete loop works without summons, generic areas, a counter, a grab, or an auto-homing dash.
- Hunt cannot be gained through empty casts or raw damage spam.
- Maul and Reconstitution create a real spend-versus-bank decision against True Body.
- All MP, cooldown, PvP, boss, ownership, and cleanup rules are explicit.
- All temporary world edits are safe and self-cleaning.
- Shader visuals have readable low-quality and failure fallbacks.
- Neutral damage is intentionally modest and skilled hunting, not passive stats, creates the advantage.
- Solo PvE, co-op, bosses, traversal, melee PvP, ranged PvP, latency, and lifecycle tests all pass.

# Explicitly out of launch scope

- Persistent beast army or pet roster.
- Gray possession.
- Fang of Rakan sentient-weapon branch.
- Fear aura or mass AI panic.
- Full terrain destruction.
- Projectile routing through rifts.
- Live target outline through walls.
- Percentage-health execute.
- Generic roar shockwave.
- Automatic regeneration.
- Continuous post-processing.
- Any ability whose final design is simply damage in a sphere.
