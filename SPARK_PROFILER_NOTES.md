# Spark Profiler Notes

## Purpose
- Track Spark profiler results while testing SoloCraft performance.
- Record TPS/MSPT, suspicious spikes, and procedures/classes that should be checked or optimized.
- Keep separate notes for idle, dungeon entry, dungeon combat, DKC, shadow stress, and return/cleanup tests.

## Profile 1 - Overworld Baseline

### Scenario
- Player did nothing, waited, and ran around in the overworld.

### Results
- TPS stayed healthy around 20.
- MSPT:
  - Minimum: 2.17 ms
  - Median: 3.25 ms
  - 95th percentile: 12.2 ms
  - Max spike: 469 ms
- Process CPU was low.
- Memory was around 953 MB / 1.9 GB.
- SoloCraft server-thread cost was about 1.00%.

### Notes
- Baseline overworld performance looks healthy.
- The 469 ms max spike is worth watching, but by itself it is probably chunk loading, saving, JVM/GC, or resource loading.
- No immediate idle TPS issue was visible.

## Profile 2 - Throne Room Dungeon Combat

### Scenario
- Player started the profiler after entering the Throne Room dungeon.
- Player fought inside the dungeon.
- Profiler was stopped after leaving the dungeon.

### Results
- Graph stayed mostly near 20 TPS during the profiled combat section.
- MSPT:
  - Minimum: 0.64 ms
  - Median: 2.91 ms
  - 95th percentile: 4.62 ms
  - Max spike: 3770 ms
- Memory was around 2.1 GB / 3.1 GB.
- SoloCraft server-thread cost was about 1.85%.

### Top SoloCraft Server Thread Entries
- `net.solocraft.entity.Portal12Entity.mobInteract()` - 0.39%
- `net.solocraft.entity.ShadowHighOrcEntity.aiStep()` - 0.27%
- `net.solocraft.__SololevelingMod_tick_ServerTickEvent.invoke()` - 0.18%
- `net.solocraft.procedures.__ShadowStorageTiersProcedure_onPlayerTick_PlayerTickEvent.invoke()` - 0.13%
- `net.solocraft.entity.DragonheadEntity.baseTick()` - 0.10%
- `net.solocraft.network.__EventBusVariableHandlers_flushPlayerVariablesSyncs_PlayerTickEvent.invoke()` - 0.08%
- `net.solocraft.entity.HighOrcEntity.baseTick()` - 0.08%
- `net.solocraft.entity.ShadowHighOrcEntity.baseTick()` - 0.05%
- `net.solocraft.procedures.__ShadowCommandTickProcedure_onLevelTick_LevelTickEvent.invoke()` - 0.04%
- `net.solocraft.procedures.__ShadowExtractionShowProcedure_onPlayerTick_PlayerTickEvent.invoke()` - 0.04%

### Notes
- Dungeon combat itself did not show sustained TPS pressure.
- The 3770 ms max spike is the main concern.
- Because the profiler included leaving the dungeon, the spike may be related to return portal interaction, dimension teleport, dungeon cleanup, chunk loading/unloading, or saving.
- `Portal12Entity.mobInteract()` should be inspected first if the leave/return spike repeats.

## Profile 3 - Lush Cave Full Dungeon Run With Shadow Stress

### Scenario
- Player started the profiler before entering Lush Cave.
- Player cleared the full dungeon.
- Player used Shadow Monarch heavily, including summoning many soldiers and extracting many new shadows.
- Profiler was stopped after leaving the dungeon.
- Lush Cave may be one of the largest single structure placements, while newer procedural dungeons place content over time.

### Results
- TPS recovered to 20 during the run.
- Rolling TPS:
  - 1 minute: 20.00
  - 5 minutes: 19.09
  - 15 minutes: 19.69
- MSPT:
  - Minimum: 0.8 ms
  - Median: 6.55 ms
  - 95th percentile: 19.7 ms
  - Max spike: 11800 ms
- Process CPU stayed low overall.
- Memory was around 2.5 GB / 3.3 GB.
- SoloCraft server-thread cost was about 11.66%.

### Top SoloCraft Server Thread Entries
- `net.solocraft.__SololevelingMod_tick_ServerTickEvent.invoke()` - 5.37%
  - Expanded cause: `net.solocraft.procedures.DunPlaceLushProcedure.lambda$execute$2()` - 4.63%
  - Expanded cause: `net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.placeInWorld()` - 4.06%
  - Expanded cause: `net.minecraft.world.level.Level.setBlock()` - 2.24%
  - Expanded cause: `net.minecraft.world.level.Level.markAndNotifyBlock()` - 1.78%
  - Expanded cause: `net.minecraft.server.level.ServerLevel.blockUpdated()` / neighbor updates - about 1.48%
  - Deep wait: chunk access through `ServerChunkCache.getChunk()` / `BlockableEventLoop.waitForTasks()` - about 1.30%
- `net.solocraft.entity.GoblinArcherEntity.baseTick()` - 1.31%
- `net.solocraft.entity.GoblinClubShadowEntity.baseTick()` - 0.90%
- `net.solocraft.entity.GoblinArcherShadowEntity.baseTick()` - 0.82%
- `net.solocraft.entity.ShadowHighOrcEntity.aiStep()` - 0.45%
- `net.solocraft.entity.GoblinClubEntity.baseTick()` - 0.31%
- `net.solocraft.entity.SteelFangedLycanEntity.baseTick()` - 0.26%
- `net.solocraft.entity.GoblinClubShadowEntity.aiStep()` - 0.20%
- `net.solocraft.procedures.__ShadowStorageTiersProcedure_onPlayerTick_PlayerTickEvent.invoke()` - 0.18%
- `net.solocraft.entity.IgrisShadowEntity.baseTick()` - 0.14%
- `net.solocraft.network.__EventBusVariableHandlers_flushPlayerVariablesSyncs_PlayerTickEvent.invoke()` - 0.14%
- `net.solocraft.entity.BeruShadowEntity.aiStep()` - 0.13%
- `net.solocraft.entity.KamishShadowEntity.aiStep()` - 0.11%
- `net.solocraft.init.__EntityAnimationFactory_onEntityTick_LivingTickEvent.invoke()` - 0.10%
- `net.solocraft.entity.GoblinArcherShadowEntity.aiStep()` - 0.10%
- `net.solocraft.entity.StoneGolemEntity.baseTick()` - 0.09%
- `net.solocraft.entity.Portal12Entity.mobInteract()` - 0.06%

### Notes
- The sustained TPS/MSPT does not look like constant shadow combat lag.
- The 11800 ms max spike is severe and should be investigated.
- Because this profile included dungeon entry, large Lush Cave structure placement, heavy shadow usage, extraction, full clear, and leaving, this run proves there is a major spike somewhere in the full dungeon loop but does not isolate the exact source.
- The expanded tree shows the biggest visible cost is Lush Cave structure placement through `DunPlaceLushProcedure`.
- A large part of the placement cost is block neighbor updates from placing the structure with update flags that notify neighbors.
- Many of the next costs are goblin and shadow entity `baseTick`/`aiStep`, which fits the heavy summon/extraction scenario. This suggests large shadow armies may become the first real sustained TPS pressure point once the big entry/exit spike is fixed.
- Most likely spike candidates are:
  - Lush Cave structure placement.
  - Dimension teleport/chunk loading during entry.
  - Return portal/dungeon cleanup during leaving.
  - Large entity spawn or mass shadow extraction burst.
- Need expanded Spark server-thread tree for this profile if available.
- First optimization target: change Lush Cave structure placement to avoid unnecessary neighbor updates during placement.

## Current Watch List

### High Priority
- Lush Cave entry/structure placement path
  - Reason: Profile 3 hit an 11800 ms max spike, and the expanded tree confirms `DunPlaceLushProcedure` / `StructureTemplate.placeInWorld()` as the main server tick cost.
  - Next test: profile only starting before entering Lush Cave and stop after the dungeon has fully loaded, after the placement flag optimization.
- `Portal12Entity.mobInteract()`
  - Reason: highest entry in Throne Room profile and may be related to dungeon leaving/return spikes.
  - Next test: profile only the return portal interaction and 10 seconds after leaving.

### Medium Priority
- Goblin and shadow entity `baseTick`/`aiStep`
  - Reason: Profile 3 showed multiple goblin and shadow entity tick costs during heavy Shadow Monarch usage.
  - Possible improvement: reduce per-tick logic, throttle expensive owner/target checks, and avoid repeated scans for each summoned soldier.
- `ShadowStorageTiersProcedure`
  - Reason: recurring player tick procedure.
  - Possible improvement: avoid running full storage checks every tick if the values do not need real-time updates.
- `EventBusVariableHandlers.flushPlayerVariablesSyncs`
  - Reason: recurring player tick sync.
  - Possible improvement: make sure variables only sync when changed.
- `ShadowCommandTickProcedure`
  - Reason: recurring level tick shadow command logic.
  - Possible improvement: throttle scans/pathing decisions and avoid global entity searches every tick.
- `ShadowExtractionShowProcedure`
  - Reason: recurring player tick procedure.
  - Possible improvement: throttle checks or only run when nearby shadow souls/extractable mobs are relevant.

### Low Priority / Monitor
- `ShadowHighOrcEntity.aiStep()`
  - Reason: top AI cost in the Throne Room profile, but still low.
  - Possible improvement: only optimize if shadow stress profiles show higher cost with many summoned units.
- `DragonheadEntity.baseTick()`
  - Reason: visible in profile, but low.
- `HighOrcEntity.baseTick()`
  - Reason: visible in profile, but low.
- `ShadowHighOrcEntity.baseTick()`
  - Reason: visible in profile, but low.

## Recommended Next Profiles

### Return/Leave Spike Test
- Start profiler before clicking the dungeon return portal.
- Click the return portal.
- Wait 10 seconds in the overworld.
- Stop profiler.
- Goal: confirm whether `Portal12Entity.mobInteract()`, dungeon cleanup, teleport, chunk loading, or variable sync causes the large spike.

### Dungeon Combat Only
- Start profiler after dungeon is fully loaded.
- Fight for 2-3 minutes.
- Stop before leaving.
- Goal: measure combat without return/cleanup mixed in.

### Dungeon Entry Test
- Start profiler before entering a gate.
- Enter the gate.
- Wait until generation and spawns finish.
- Stop profiler.
- Goal: measure generation, teleport delay, room placement, and initial mob spawning.

### Shadow Stress Test
- Summon many shadows.
- Use Shadow Command modes.
- Fight groups of mobs for 2-3 minutes.
- Goal: measure shadow AI, command logic, inventory pickup, kill credit, and pathfinding.

### DKC Test
- Start profiler before entering or changing DKC floors.
- Change floor and fight a boss or group.
- Stop after the floor stabilizes.
- Goal: measure DKC floor generation/spawning, kill requirements, boss logic, and floor notifications.
