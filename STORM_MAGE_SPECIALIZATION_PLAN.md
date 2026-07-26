# Storm Mage Specialization Plan

Status: implemented for build verification and gameplay tuning. All eight skills,
Storm Current, runestones, generated-hunter use, and Baran inheritance are wired;
final coefficients remain intentionally tuneable after combat testing.

## Why Storm Mage

The current Mage specializations already cover three strong identities:

| Specialization | Core loop | Strength |
| --- | --- | --- |
| Fire Mage | Apply and consume Scorch | Direct damage, executions, explosions, area denial |
| Barrier Mage | Build and break constructs for Resonance | Protection, displacement, reflection, battlefield control |
| Arcane Mage | Assemble Formula runes and enter Overcast | Precision, mobility, spatial utility, flexible spell chains |

The missing role is a fast ranged skirmisher. Storm Mage fills it through movement, target routing, and controlled chain damage. It should feel agile and deliberate rather than becoming Fire Mage with yellow particles.

Its palette is gold-white lightning over deep navy storm effects. It does not create vanilla lightning, alter weather, mutate blocks, or require permanent entities.

## Combat identity

Storm Mage is strongest when the player keeps moving, tags the correct enemy, and routes attacks through a group. It has:

- Better mobility and multi-target routing than the other Mage types.
- Lower raw area burst than Fire Mage.
- No durable protection comparable to Barrier Mage.
- Less general-purpose utility than Arcane Mage.
- Soft control only; it never relies on long stuns.

Hunter Rank controls which skills are unlocked. Intelligence and System Level control output. Rank must never directly multiply spell damage.

## Passive: Storm Current

Storm Mage has a private **Voltage** meter from 0 to 100.

- Moving while actively in combat generates up to 8 Voltage per second.
- A direct Storm spell hit grants 8 Voltage.
- Additional targets hit by the same cast grant 2 Voltage each, with a 14 Voltage cap per cast.
- Voltage begins decaying after four seconds without combat activity.
- Voltage cannot be safely pre-charged outside combat.
- At 100 Voltage, the next eligible offensive spell becomes **Overcharged** and consumes the meter.
- Utility skills cannot accidentally spend Overcharge.

**Conductive** is a six-second, owner-scoped mark applied by Static Needle. It deals no damage and does not stack. It only gives later Storm spells better routing targets.

The Voltage meter belongs beside the ability HUD. It must not add another indicator around the crosshair.

## Progression

Storm Mage uses the existing Intelligence breakpoints:

| Intelligence stage | Name | Design purpose |
| --- | --- | --- |
| 1 | Spark | Base behavior |
| 30 | Current | Range and consistency |
| 55 | Surge | Limited piercing, chaining, or charge utility |
| 80 | Tempest | Stronger routing and Overcharge behavior |
| 110 | Stormborn | Final mechanical evolution, not a simple damage multiplier |

### Baran inheritance and the sixth stage

Awakening Baran, Monarch of White Flames, immediately grants three inherited
Storm techniques without changing the player's Hunter class or Mage
specialization:

- Static Needle
- Slipstream
- Chain Lightning

This is a small mark/reposition/payoff package rather than the full Storm Mage
kit. A native Storm Mage who also becomes Baran still owns all eight skills.
Losing or resetting the Baran vessel entitlement removes only borrowed Storm
skills; native Storm Mage ownership is preserved.

While White-Flame Spiritualization is active, Storm output is calculated
dynamically rather than written into permanent progression:

- Spark through Tempest temporarily borrow the next output stage, capped at
  Stormborn.
- A full-mastery caster with Intelligence strictly above 110 unlocks the
  exclusive sixth stage, Sovereign Tempest.
- At exactly 110 Intelligence the caster remains Stormborn.
- Turning Spiritualization off immediately restores the natural stage.

The temporary calculation never mutates Intelligence, unlock tiers, or saved
mastery data.

The intended unlock path is:

| Unlock | Ability | Mana | Cooldown |
| --- | --- | ---: | ---: |
| E Rank | Static Needle | 0.35% max MP | 9 ticks |
| D Rank | Slipstream | 2% max MP | 4 seconds |
| D Mastery | Thunderclap | 3.25% max MP | 6 seconds |
| C Rank | Lightning Rod | 4% max MP | 10 seconds |
| B Rank | Chain Lightning | 8% max MP | 14 seconds |
| A Rank | Thunderhead | 11% max MP | 24 seconds |
| S Rank | Skybreaker | 13% max MP | 30 seconds |
| S Mastery | Tempest Incarnate | 18% max MP | 60 seconds |

Exact damage coefficients should be tuned against the current Fire, Barrier, and Arcane managers during implementation.

## Ability design

### Static Needle

A fast precision bolt with modest single-target damage. It applies Conductive and is the reliable Voltage builder.

- Current increases range.
- Surge adds one reduced-damage pierce.
- Tempest adds a delayed return spark if the original bolt hit.
- Overcharged Static Needle forks once to a nearby Conductive target.
- It never becomes an explosion or a free-spamming machine gun.

This is an immediate skill, not a QTE.

### Slipstream

A collision-safe directional dash performed over four ticks, preserving visible motion instead of teleporting.

- Uses movement input when available and look direction as fallback.
- Passing close to one Conductive target grants a small once-per-dash Voltage bonus.
- Current cleanses Slowness.
- Tempest stores a second charge.
- Cannot pass through solid walls or target unloaded chunks.

This is an immediate skill and never spends Overcharge.

### Thunderclap

An emergency 100-degree cone that deals moderate damage and pushes enemies away.

- Conductive normal mobs receive a brief heavy slow.
- Bosses receive only a very short soft slow.
- Players cannot be hard-stunned or forcibly launched.
- Overcharge converts the cone to a wider radial peel instead of multiplying its damage.

This is the D-rank mastery skill and remains immediate.

### Lightning Rod

Marks one aimed enemy for 12 seconds. Recasting relocates the mark.

- Storm skills prioritize the Rod target as their root.
- Hitting it generates slightly more Voltage.
- The mark does not increase all incoming damage, preventing party stacking abuse.
- Invalid or obstructed casts consume neither mana nor cooldown.
- Only the caster sees the full outline; party visibility may be added later.

This is an immediate utility skill and never spends Overcharge.

### Chain Lightning

A controlled multi-target cleave that hits up to five unique visible enemies.

- Conductive targets are prioritized.
- Damage falls by roughly 18% per jump.
- Each cast has a hard total-damage budget.
- Overcharge creates two branches, but both share the same target count and damage budget.
- A target cannot be hit twice by branches from one cast.

This is an immediate skill. The server resolves it with raycasts and a bounded target list; visual arcs are client effects.

### Thunderhead

Places one short-lived storm over the aimed area for about seven seconds.

- Performs five or six scheduled strikes instead of scanning every tick.
- Prioritizes Conductive targets and avoids repeating one target while alternatives exist.
- Overcharge adds one bounded final thunderburst.
- Only one Thunderhead may exist per owner; recasting replaces the old one.

This uses the Mage QTE system because placement and commitment matter.

### Skybreaker

A clearly telegraphed vertical finisher placed on the Lightning Rod target or aimed ground point.

- Focuses on strong single-target damage rather than Heavenfall-sized area damage.
- Nearby Conductive enemies receive small fork damage.
- Overcharge adds two delayed reduced-damage echoes under one fixed total-output cap.
- The warning column must be visible enough for PvP reaction.

This uses the Mage QTE system.

### Tempest Incarnate

A ten-second gold-white lightning cloak:

- Grants 15% movement speed.
- Grants 50% more Voltage generation.
- Gives Slipstream a second temporary charge.
- Every third damaging Storm cast creates one 35%-damage echo.
- Allows at most five echoes and two Overcharges per activation.
- Grants no invulnerability, free spells, cooldown resets, or blanket damage multiplier.

This is the S-rank mastery and uses the Mage QTE system.

## Multiplayer rules

- Conductive, Lightning Rod, Voltage, Thunderhead, and transformation state are owner-scoped.
- Chains exclude allies, party members, pets, and shadows.
- Chain and echo damage receive an initial 35% PvP reduction.
- Bosses and players ignore hard control; only short soft slows apply.
- Multiple Storm Mages may mark the same enemy without sharing or multiplying marks.
- Target selection requires line of sight for the initial hit and every chain jump.

## Performance contract

- Never spawn vanilla lightning entities or alter weather.
- Never mutate blocks, hold chunk tickets, or target unloaded chunks.
- Maximum six candidates resolved per chain and one area scan per scheduled strike.
- One Lightning Rod, one Thunderhead, and one Tempest state per owner.
- Thunderhead selects targets only on strike ticks.
- Lightning visuals use short-lived client ribbons with distance-based branch and flash limits.
- Voltage sync occurs only on material changes or threshold crossings, capped near 5 Hz.
- All owner state is removed on logout, death, dimension change, or specialization change.

## Implementation map

1. Add `storm` to specialization validation, random assignment, display names, tier lists, evaluation grants, and mastery grants in `MageSpellProgression`.
2. Add `StormMageSpellManager` with server-authoritative state, bounded target helpers, and cleanup events.
3. Register the Storm category and eight specialization-validated runestones.
4. Add immediate dispatch for the practical core and QTE dispatch only for Thunderhead, Skybreaker, and Tempest Incarnate.
5. Add HUD icons, a compact Voltage bar, target outline, localization, and lightweight lightning ribbons.
6. Add generated Storm Mage hunter behavior with conservative cast frequency and the same target caps.
7. Add deterministic tests for ownership, mana/cooldown failure behavior, chain uniqueness, PvP exclusions, state cleanup, and output caps.
8. Profile Thunderhead and Chain Lightning with several Storm Mages before final coefficient tuning.

The implementation should reuse `MageSpellProgression`, `MageCombatHelper`, `MageQTEHelper`, `CooldownManager`, existing private target outlines, and the lightweight synced VFX-carrier pattern. It should not reuse the retired generic Mage spell implementations.
