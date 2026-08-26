package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Dependency-free regressions for the July dungeon bug-report batch. */
public final class DungeonBugfixRegression {
	private static final Path MAIN_SOURCE = Path.of(
			"src", "main", "java", "net", "solocraft");

	private DungeonBugfixRegression() {
	}

	public static void main(String[] args) throws IOException {
		returnPortalsUseAuthoredEntranceAnchors();
		dkcPermitPedestalRepairsImmediatelyAndForExistingSaves();
		gatePreparationNeverStrandsPlayersInVoid();
		proceduralGateFailuresAreRecoverable();
		orphanedShadowsFailClosedAcrossDimensions();
		systemXpRewardsGrantTheirDisplayedValue();
		legacyStuckCommandUsesCurrentRecovery();
		jobChangeKnightDamageIsEncounterOnly();
		environmentalDeathsKeepShadowOwnerCredit();
		pathologicalGoblinHealthIsContained();
		lowRankDungeonsAreActuallyIntroductory();
		snowRedGateMobsRespectShadowThreatAndDamageLimits();
		structurePlacementNeverRequestsNeighborUpdates();
		clearDungeonAdvancesOnTheBossWhenNothingIsNear();
	}

	private static void dkcPermitPedestalRepairsImmediatelyAndForExistingSaves()
			throws IOException {
		String builder = read("dkc", "DkcFloorBuilder.java");
		String boss = read("procedures", "DKCBossKillRewardProcedure.java");
		String repair = builder.substring(
				builder.indexOf("public static boolean ensurePermitPedestal"),
				builder.indexOf("public static void tickPlayer"));
		expectTrue(repair.contains("vars.dkc_cleared < floor")
						&& repair.contains("!runs.isGenerated(player, floor)")
						&& repair.contains("runs.isTransitionArmed(player, floor)")
						&& repair.contains("runs.isGenerated(player, floor + 1)"),
				"A permit pedestal may repair only for a cleared, generated floor whose path is not already open");
		expectTrue(repair.contains("level.getChunkAt(pedestal)")
						&& repair.contains("level.setBlock(pedestal,")
						&& repair.contains("DEEPSLATE_KEYBLOCK_DKC"),
				"Pedestal recovery must load its exact chunk and restore the progression block");
		expectTrue(boss.contains(
						"DkcFloorBuilder.ensurePermitPedestal(player, floor);")
						&& builder.substring(builder.indexOf("public static void tickPlayer"),
								builder.indexOf("private static void tickReturnSigil"))
								.contains("ensurePermitPedestal(player, floor)"),
				"Boss completion must repair immediately and the floor tick must recover already-affected saves");
	}

	/**
	 * Clear Dungeon must sweep, then push. Shadows previously stopped dead once
	 * nothing was reachable in range, so a cleared corridor left the whole group
	 * standing still instead of moving on to the boss.
	 */
	private static void clearDungeonAdvancesOnTheBossWhenNothingIsNear()
			throws IOException {
		String manager = Files.readString(
				MAIN_SOURCE.resolve("util").resolve("ShadowMonarchManager.java"));
		expectTrue(manager.contains("CLEAR_ENGAGE_RADIUS_SQR"),
				"Clear Dungeon needs a sweep radius so nearby monsters are fought first");
		expectTrue(manager.contains("selectClearDungeonObjective")
						&& manager.contains("advanceTowardObjective"),
				"Clear Dungeon needs a boss objective and a way to advance on it");
		expectTrue(manager.contains("isClearDungeonBoss"),
				"Clear Dungeon must be able to identify the boss");

		int assign = manager.indexOf("private static void assignClearDungeonTarget");
		expectTrue(assign > 0, "assignClearDungeonTarget must exist");
		int assignEnd = manager.indexOf("\n\tprivate static", assign + 1);
		String body = manager.substring(assign, assignEnd < 0 ? manager.length() : assignEnd);
		int objectiveCall = body.indexOf("selectClearDungeonObjective");
		expectTrue(objectiveCall > 0,
				"Running out of nearby targets must fall through to the boss objective");
		// The old failure mode: the sweep gave up straight into an idle stop. Any
		// halt must therefore come after the objective has been considered, never
		// before it.
		int firstStop = body.indexOf("getNavigation().stop()");
		expectTrue(firstStop < 0 || firstStop > objectiveCall,
				"Shadows must reach the boss objective before any idle stop");

		int progress = manager.indexOf("private static boolean tickClearTargetProgress");
		int progressEnd = manager.indexOf("\n\tprivate static", progress + 1);
		String progressBody = manager.substring(progress,
				progressEnd < 0 ? manager.length() : progressEnd);
		expectTrue(progressBody.contains("isClearDungeonBoss"),
				"The stuck watchdog must never blacklist the boss objective");
		expectTrue(manager.contains("tickClearTraversal")
					&& manager.contains("progress.beginTraversal")
					&& manager.contains("path == null || !path.canReach()")
					&& manager.contains("tryRecallShadowNearOwner"),
				"Partial paths must remain travel objectives, with safe recovery, instead of becoming wall-facing combat targets");
	}

	/**
	 * Structure placement must never pass the UPDATE_NEIGHBORS bit.
	 *
	 * <p>A neighbour update makes each block on a template edge read the block
	 * beside it; if that block sits in an unloaded chunk the server thread blocks
	 * inside getChunkBlocking until the chunk generates. A live server hung for
	 * 60 seconds generating a dungeon 200k blocks out because every placeInWorld
	 * call passed flag 3.
	 */
	private static void structurePlacementNeverRequestsNeighborUpdates()
			throws IOException {
		List<Path> offenders = new ArrayList<>();
		try (Stream<Path> sources = Files.walk(MAIN_SOURCE)) {
			for (Path source : sources.filter(path -> path.toString().endsWith(".java"))
					.toList()) {
				String text = Files.readString(source);
				int cursor = text.indexOf("placeInWorld");
				while (cursor >= 0) {
					int open = text.indexOf('(', cursor);
					if (open < 0)
						break;
					int depth = 1;
					int index = open + 1;
					while (index < text.length() && depth > 0) {
						char character = text.charAt(index);
						if (character == '(')
							depth++;
						else if (character == ')')
							depth--;
						index++;
					}
					String arguments = text.substring(open + 1, index - 1);
					int lastComma = topLevelLastComma(arguments);
					if (lastComma >= 0) {
						String flag = arguments.substring(lastComma + 1).trim();
						// Any odd literal carries UPDATE_NEIGHBORS (bit 0).
						if (flag.matches("\\d+") && (Integer.parseInt(flag) & 1) != 0)
							offenders.add(source);
					}
					cursor = text.indexOf("placeInWorld", index);
				}
			}
		}
		expectTrue(offenders.isEmpty(),
				"placeInWorld must not request neighbor updates: " + offenders);
	}

	private static int topLevelLastComma(String arguments) {
		int depth = 0;
		int last = -1;
		for (int index = 0; index < arguments.length(); index++) {
			char character = arguments.charAt(index);
			if (character == '(')
				depth++;
			else if (character == ')')
				depth--;
			else if (character == ',' && depth == 0)
				last = index;
		}
		return last;
	}

	private static void returnPortalsUseAuthoredEntranceAnchors()
			throws IOException {
		String generator = read("dungeon",
				"ProceduralDungeonGenerator.java");
		String gate = read("dungeon",
				"ProceduralDungeonGateHandler.java");
		String completion = read("dungeon",
				"ProceduralDungeonCompletionHandler.java");
		String boss = read("procedures", "BossKilledProcedure.java");
		String runtime = read("dungeon", "runtime",
				"DungeonEncounterRuntime.java");
		String baruka = read("procedures",
				"BarukaEntityDiesProcedure.java");
		String legacyTeleport = read("procedures",
				"DungeonTeleportAndSpawnProcedure.java");

		expectTrue(generator.contains(
						"BlockPos portalPos = returnPortalPosition(")
				&& generator.contains(
						"if (spawnInitialReturnPortal)"),
				"The entry return anchor must exist even while its portal is deferred");
		expectTrue(gate.contains(
						"recordUnscopedReturnAnchor(")
				&& completion.contains(
						"RunView::returnAnchor"),
				"Built-in procedural runs must durably retain their authored return point");
		expectTrue(boss.contains(
						"resolveUnscopedReturnPosition(")
				&& !boss.contains(
						"spawnUnscopedReturnPortal(\n\t\t\t\t\t\t\tlevel, entity.blockPosition()"),
				"Boss completion must not place an exit at the corpse");
		expectTrue(runtime.contains(
						"instance.exit()\n\t\t\t\t\t.or(() -> instance.playerStart())")
				&& !runtime.contains(
						"? defeatedBoss.blockPosition()"),
				"Runtime dungeons must reveal their authored exit instead of moving it to the boss");
		expectTrue(completion.contains(
						"public static boolean loadReturnPortalChunk(")
						&& completion.contains("level.getChunkAt(position)")
						&& completion.contains(
								"if (!loadReturnPortalChunk(level, desired))")
						&& runtime.contains(
								"loadReturnPortalChunk(level,"),
				"Boss completion must load the authored exit chunk immediately instead of waiting for a player to walk backward");
		expectFalse(baruka.contains("PORTAL_12"),
				"Baruka death must not create a second corpse-position portal");
		int proceduralGuard = legacyTeleport.indexOf(
				"ProceduralDungeonCompletionHandler.PROCEDURAL_DUNGEON_TAG");
		int portalSearch = legacyTeleport.indexOf(
				"world.getEntitiesOfClass(Portal12Entity.class");
		expectTrue(proceduralGuard >= 0 && portalSearch > proceduralGuard
					&& legacyTeleport.contains("world.isClientSide()"),
				"Revealing a procedural return portal must never trigger the legacy entry teleporter");
		expectFalse(boss.contains("teleportTo("),
				"Boss completion may reveal an exit but must not move any player away from loot");
	}

	private static void gatePreparationNeverStrandsPlayersInVoid()
			throws IOException {
		String procedural = read("dungeon",
				"ProceduralDungeonGateHandler.java");
		String datapack = read("dungeon",
				"DatapackDungeonGateHandler.java");
		String redGate = read("dungeon", "runtime",
				"SnowRedGateArenaManager.java");
		expectFalse(procedural.contains("entrant.setNoGravity(true)")
						|| datapack.contains("entrant.setNoGravity(true)")
						|| redGate.contains("entrant.setNoGravity(true)"),
				"Normal gate preparation must not leave players floating if the gate is cleared concurrently");
		expectFalse(procedural.contains(
						"entrant.teleportTo(nextLevel, targetPos.getX()"),
				"Procedural entrants must remain beside the gate instead of entering an empty build anchor");
		int generation = procedural.indexOf(
				"ProceduralDungeonGenerator.generate(");
		int entryTeleport = procedural.indexOf(
				"entrant.teleportTo(nextLevel, startX");
		expectTrue(generation >= 0 && entryTeleport > generation,
				"Players may enter a procedural dimension only after its safe start exists");
		expectTrue(procedural.contains("entrant.setNoGravity(false)")
						&& datapack.contains("entrant.setNoGravity(false)")
						&& redGate.contains(
								"recoverLostHandoffEntrants("),
				"Success and failure cleanup must still recover stale gravity state");
	}

	private static void proceduralGateFailuresAreRecoverable()
			throws IOException {
		String gate = read("dungeon",
				"ProceduralDungeonGateHandler.java");
		expectTrue(gate.contains(
						"nextLevel.getChunk(targetPos.getX() >> 4")
						&& gate.contains(
								"nextLevel.getChunk(startPos.getX() >> 4"),
				"Remote procedural generation and arrival chunks must be loaded before use");
		expectTrue(gate.contains("catch (RuntimeException exception)")
						&& gate.contains("clearEntrantBinding(entrant)")
						&& gate.contains("clearPreparing(gate)"),
				"A generation exception must release both the player binding and the shared gate lock");
		expectTrue(gate.contains("recoverInterruptedBinding(player)")
						&& gate.contains("STALE_PREPARATION_TICKS"),
				"Interrupted entries must self-repair instead of poisoning every later regular gate");
		expectTrue(gate.contains("gate instanceof Portal1Entity")
						&& gate.contains(
								"ensureProceduralMetadata(gate)"),
				"Legacy unnamed Portal1 gates must migrate into the working procedural path");
		expectTrue(gate.contains(
						">= ProceduralDungeonRank.B.numericRank"),
				"An introductory low-rank procedural gate must not transform into a Monarch Red Gate");
	}

	private static void orphanedShadowsFailClosedAcrossDimensions()
			throws IOException {
		String manager = read("util",
				"ShadowMonarchManager.java");
		String dimension = read("procedures",
				"DimensionChangeStatResetProcedure.java");
		String igris = read("procedures",
				"IgrisShadowOnEntityTickUpdateProcedure.java");
		String sharedTick = read("procedures",
				"CommandCallProcedureProcedure.java");
		int guard = igris.indexOf(
				"handleUnavailableShadowOwner(entity)");
		int legacyOwnerRead = igris.indexOf(
				"_tamEnt.getOwner() : null).isAlive()");
		expectTrue(manager.contains(
						"dismissLoadedOwnedShadows(ServerPlayer owner")
						&& manager.contains(
								"findOnlineOwner(level, ownerId)")
						&& dimension.contains(
								"dismissLoadedOwnedShadows(player,"),
				"Dimension changes must dismiss all loaded summons using the cross-dimension owner lookup");
		expectTrue(guard >= 0 && (legacyOwnerRead < 0
						|| guard < legacyOwnerRead)
						&& sharedTick.contains(
								"handleUnavailableShadowOwner(entity)"),
				"Every generated and shared shadow tick must stop before using an unavailable owner");
	}

	private static void systemXpRewardsGrantTheirDisplayedValue()
			throws IOException {
		String collect = read("procedures",
				"RewardCollectProcedure.java");
		String xp = read("procedures", "XPGainProcedure.java");
		expectTrue(collect.contains(
						"XPGainProcedure.awardRewardXp(player, amount)")
						&& !collect.contains(
								"XPGainProcedure.awardBaseXp(entity.level(), player, amount)"),
				"Claimed System XP must use fixed reward semantics rather than combat scaling");
		expectTrue(xp.contains(
						"public static void awardRewardXp(Player player, int rewardXP)")
						&& xp.contains("capability.Xp += rewardXP"),
				"A displayed 420 XP reward must add exactly 420 XP on every difficulty");
	}

	private static void legacyStuckCommandUsesCurrentRecovery()
			throws IOException {
		String command = read("command",
				"SoloDungeonStuckCommand.java");
		String bridge = read("procedures",
				"SoloDungeonStuckProcedureProcedure.java");
		String recovery = read("procedures",
				"DungeonDimensionPlayerLeavesDimensionProcedure.java");
		expectTrue(command.contains("getPlayerOrException()")
						&& command.contains("return 1;"),
				"The legacy command must execute as its real player and report success");
		expectTrue(bridge.contains(
						"DungeonDimensionPlayerLeavesDimensionProcedure.emergencyExit(player)")
						&& !bridge.contains("deleted_mod_element"),
				"Recovery must not depend on a stale hard-coded dimension list");
		expectTrue(recovery.contains("findSafeOverworldReturn(")
						&& recovery.contains("isSafeStandingPosition("),
				"Dungeon recovery must validate its overworld landing");
	}

	private static void jobChangeKnightDamageIsEncounterOnly()
			throws IOException {
		String spawner = read("procedures",
				"SpawnerPortalOnEntityTickUpdateProcedure.java");
		String balance = read("util",
				"JobChangeKnightBalance.java");
		expectTrue(spawner.contains(
						"JobChangeKnightBalance.markAndBalance(spawned)"),
				"Only portal-created Job Change knights should receive the tuning");
		expectTrue(balance.contains(
						"slr_job_change_advancement_knight")
						&& balance.contains(
								"AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL")
						&& balance.contains("DAMAGE_MULTIPLIER = -0.60D"),
				"The oversized weapon-inclusive damage must be durably reduced");
	}

	private static void environmentalDeathsKeepShadowOwnerCredit()
			throws IOException {
		String helper = read("procedures",
				"ShadowKillCreditHelper.java");
		String xp = read("procedures", "XPGainProcedure.java");
		String boss = read("procedures", "BossKilledProcedure.java");
		String runtime = read("dungeon", "runtime",
				"DungeonEncounterRuntime.java");
		expectTrue(helper.contains("rememberRecentPlayerDamage(")
						&& helper.contains("creditedPlayerForDeath(")
						&& helper.contains("victim.getKillCredit()"),
				"Recent shadow damage must survive an environmental final hit");
		expectTrue(xp.contains("creditedPlayerForDeath(")
						&& !xp.contains("DamageTypeTags.IS_FIRE"),
				"Player XP fallback must cover fall/void/collision deaths, not only fire");
		expectTrue(boss.contains("creditedSourceForDeath(")
						&& runtime.contains("creditedSourceForDeath("),
				"Both built-in and runtime boss completion must use preserved shadow-owner credit");
	}

	private static void pathologicalGoblinHealthIsContained()
			throws IOException {
		String guard = read("dungeon",
				"DungeonMobHealthCompatibilityGuard.java");
		String generator = read("dungeon",
				"ProceduralDungeonGenerator.java");
		String adapter = read("dungeon", "runtime",
				"DungeonMobLevelAdapter.java");
		expectTrue(guard.contains(
						"PATHOLOGICAL_VALUE = 1_000_000.0D")
						&& guard.contains("GoblinClubEntity")
						&& guard.contains("maxHealth.removeModifier(modifier)")
						&& guard.contains(
								"Math.min(living.getHealth(),"),
				"Million-scale foreign goblin health must be removed and current HP clamped");
		expectTrue(generator.contains(
						"DungeonMobHealthCompatibilityGuard.stabilize(entity)")
						&& adapter.contains(
								"DungeonMobHealthCompatibilityGuard.stabilize(mob)"),
				"Both built-in and datapack dungeon spawns must run the compatibility guard");
	}

	private static void lowRankDungeonsAreActuallyIntroductory()
			throws IOException {
		String ranks = read("dungeon", "ProceduralDungeonRank.java");
		String generator = read("dungeon",
				"ProceduralDungeonGenerator.java");
		String balance = read("dungeon", "LowRankDungeonBalance.java");
		String sewers = read("procedures", "RandomCorridor1Procedure.java");
		String gateSpawner = read("util", "GateSpawnerUtil.java");

		expectTrue(ranks.contains("E(1, 7, 11, 13, 17, 5, 4, 2)")
						&& ranks.contains("D(2, 8, 12, 15, 19, 5, 5, 2)"),
				"E/D procedural dungeons need meaningful layouts and multi-enemy packs");
		expectTrue(generator.contains("packsForRoom(room, settings, random)")
						&& generator.contains("packSizeFor(settings, random)")
						&& generator.contains("return Math.min(3, packs)"),
				"Low-rank density variation must remain explicitly bounded");
		int ePoolStart = generator.indexOf("case E -> List.of(");
		int dPoolStart = generator.indexOf("case D -> List.of(", ePoolStart);
		int cPoolStart = generator.indexOf("case C -> List.of(", dPoolStart);
		expectTrue(ePoolStart >= 0 && dPoolStart > ePoolStart
						&& cPoolStart > dPoolStart,
				"The low-rank procedural mob pools must remain explicit");
		String ePool = generator.substring(ePoolStart, dPoolStart);
		String dPool = generator.substring(dPoolStart, cPoolStart);
		expectFalse(ePool.contains("STEEL_FANGED_LYCAN"),
				"E rank must not roll the 30-health, 8-damage Lycan");
		expectTrue(dPool.contains("GOBLIN_MAGE.get(), 18")
						&& dPool.contains("STEEL_FANGED_LYCAN.get(), 10"),
				"D rank must heavily reduce its ranged and Lycan danger rolls");

		expectTrue(balance.contains("E_HEALTH_MULTIPLIER = 0.65D")
						&& balance.contains("D_HEALTH_MULTIPLIER = 0.80D")
						&& balance.contains("E_DAMAGE_MULTIPLIER = 0.55F")
						&& balance.contains("D_DAMAGE_MULTIPLIER = 0.70F")
						&& balance.contains("AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL")
						&& balance.contains("event.setAmount(event.getAmount() * multiplier)"),
				"E/D health and final outgoing damage need durable rank caps");
		expectTrue(sewers.contains("ELITE_COMPONENT = 2")
						&& sewers.contains("FINAL_COMBAT_COMPONENT = 4")
						&& sewers.contains("\"erankbig2\"")
						&& !sewers.contains("ResourceLocation.fromNamespaceAndPath(\"sololeveling\", \"erankbig1\")"),
				"Goblin Sewers must be shorter and use its lighter elite room");
		expectTrue(gateSpawner.contains(
				"NaturalGateChoice.entity(75, ProceduralDungeonRank.D"),
				"The D-rank Goblin Sewers gate must not unlock for E-rank hunters");

		String attack1 = read("procedures", "GoblinBossattack1Procedure.java");
		String attack2 = read("procedures", "GoblinBossattack2Procedure.java");
		String dash = read("procedures", "GoblinBossDashProcedure.java");
		String smash = read("procedures", "GoblinBossSmashProcedure.java");
		expectTrue(attack1.contains("DamageTypes.MOB_ATTACK), entity")
						&& attack2.contains("DamageTypes.MOB_ATTACK), entity")
						&& attack2.contains("_level.explode(entity,")
						&& dash.contains("DamageTypes.GENERIC), entity")
						&& smash.contains("DamageTypes.GENERIC), entity")
						&& !dash.contains("DamageTypes.GENERIC))")
						&& !smash.contains("DamageTypes.GENERIC))"),
				"Every Goblin King scripted hit must identify its attacker so Tank defenses apply");
	}

	private static void snowRedGateMobsRespectShadowThreatAndDamageLimits()
			throws IOException {
		String iceElf = read("entity", "IceElfEntity.java");
		String polarBear = read("entity", "PolarBearEntity.java");
		String arrows = read("procedures",
				"IceElfOnEntityTickUpdateProcedure.java");
		String threat = read("entity", "ai",
				"ShadowThreatTargetGoal.java");

		expectTrue(iceElf.contains(
						"targetSelector.addGoal(0, shadowThreatGoal)")
						&& polarBear.contains(
								"targetSelector.addGoal(0, shadowThreatGoal)")
						&& iceElf.contains(
								"shadowThreatGoal.recordSuccessfulHit(source")
						&& polarBear.contains(
								"shadowThreatGoal.recordSuccessfulHit(source"),
				"Both snow-red-gate mobs must allow accumulated shadow damage to preempt player aggro");
		expectTrue(threat.contains("HITS_TO_PULL_AGGRO = 2")
						&& threat.contains(
								"GROUP_HITS_TO_PULL_AGGRO = 3")
						&& threat.contains("TARGET_LOCK_TICKS = 120")
						&& threat.contains(
								"ShadowMonarchManager.isShadowEntity")
						&& threat.contains(
								"ShadowMonarchManager.isTrackedShadowEntity")
						&& threat.contains("mob.setTarget(attacker)"),
				"Shadow retaliation must recognize the full summon roster and hold aggro long enough to matter");
		expectTrue(iceElf.contains(
						"builder.add(Attributes.ATTACK_DAMAGE, 2)")
						&& polarBear.contains(
								"builder.add(Attributes.ATTACK_DAMAGE, 8)")
						&& !polarBear.contains(
								"builder.add(Attributes.ATTACK_DAMAGE, 22)"),
				"Ice elves and custom polar bears must use the reduced melee damage baseline");
		expectTrue(iceElf.contains(
						"SLRIceElfCombatBalanceVersion")
						&& iceElf.contains(
								"LEGACY_ATTACK_DAMAGE_REDUCTION = 1.0D")
						&& polarBear.contains(
								"SLRPolarBearCombatBalanceVersion")
						&& polarBear.contains(
								"LEGACY_ATTACK_DAMAGE_REDUCTION = 14.0D")
						&& iceElf.contains(
								"compound.putInt(COMBAT_BALANCE_VERSION_TAG")
						&& polarBear.contains(
								"compound.putInt(COMBAT_BALANCE_VERSION_TAG"),
				"Previously saved snow-red-gate mobs must migrate away from their serialized legacy damage once");
		expectTrue(arrows.contains("ARROW_BASE_DAMAGE = 1.5D")
						&& arrows.contains("ARROW_SPEED = 2.35F")
						&& arrows.contains("setCritArrow(false)")
						&& !arrows.contains("setCritArrow(true)"),
				"Ice-elf arrows must not retain the old speed-three critical damage spike");
	}

	private static String read(String directory, String file)
			throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory).resolve(file)).replace("\r\n", "\n");
	}

	private static String read(String directory, String subdirectory,
			String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory)
				.resolve(subdirectory).resolve(file)).replace("\r\n", "\n");
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}
}
