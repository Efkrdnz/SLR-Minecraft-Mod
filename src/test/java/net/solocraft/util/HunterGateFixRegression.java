package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Dependency-free regression checks for Hunter damage, goblin retaliation, and
 * legacy procedural gate cleanup.
 */
public final class HunterGateFixRegression {
	private static final Path MAIN_SOURCE = Path.of("src", "main", "java", "net", "solocraft");

	private HunterGateFixRegression() {
	}

	public static void main(String[] args) throws IOException {
		gateCompletionTokensAreExact();
		legacyHunterDefenseSubscribersStayRemoved();
		starterGoblinFamilyRetaliatesWhenHurt();
		gateCleanupUsesStableServerTicksAndPreservesDungeonTags();
		returnPortalLifecycleMatchesGateType();
	}

	private static void gateCompletionTokensAreExact() {
		expectTrue(GateCompletionTokens.contains("first,target,last,", "target"),
				"An exact middle gate token must be found");
		expectFalse(GateCompletionTokens.contains("prefix-target,last,", "target"),
				"A gate id contained inside another token must not match");
		expectEquals("first,last,", GateCompletionTokens.remove("first,target,last,", "target"),
				"Removing one gate must preserve both neighboring tokens");
		expectEquals("prefix-target,last,", GateCompletionTokens.remove("prefix-target,last,", "target"),
				"Removing a substring must leave the list unchanged");
		expectEquals("", GateCompletionTokens.remove("target,target,", "target"),
				"Duplicate exact completion tokens must all be consumed");
	}

	private static void legacyHunterDefenseSubscribersStayRemoved() throws IOException {
		for (String procedure : List.of(
				"AssassinDamageDealProcedure.java",
				"FighterDamageDealProcedure.java",
				"TankerDamageDealProcedure.java")) {
			String source = read("procedures", procedure);
			expectTrue(source.contains("if (sourceentity instanceof HunterEntity)"),
					procedure + " must retain outgoing Hunter class effects");
			expectFalse(source.contains("if (entity instanceof HunterEntity)"),
					procedure + " must not restore its duplicate victim-side defense");
			expectFalse(source.contains("event.setCanceled(true)"),
					procedure + " must leave defensive cancellation to HunterHurtProcedure");
		}
		String centralizedDefense = read("procedures", "HunterHurtProcedure.java");
		expectTrue(centralizedDefense.contains("HunterAIHelper.tryDefensiveReaction"),
				"HunterHurtProcedure must remain the sole class-aware defensive entry point");
	}

	private static void starterGoblinFamilyRetaliatesWhenHurt() throws IOException {
		for (String entity : List.of(
				"GoblinClubEntity.java",
				"GoblinArcherEntity.java",
				"GoblinMageEntity.java",
				"GoblinKingEntity.java")) {
			String source = read("entity", entity);
			expectTrue(source.contains("addGoal(0, new HurtByTargetGoal(this))"),
					entity + " must prioritize retaliation against a Hunter NPC that hurts it");
		}
	}

	private static void gateCleanupUsesStableServerTicksAndPreservesDungeonTags() throws IOException {
		String source = read("procedures", "IsGateClearedProcedure.java");
		expectTrue(source.contains("entity.level().isClientSide()"),
				"Gate completion polling must be server-only");
		expectTrue(source.contains("entity.tickCount % 20 != 0"),
				"Gate completion polling must use entity ticks");
		expectFalse(source.contains("world.dayTime()"),
				"Frozen daylight must not be able to disable gate completion polling");
		expectFalse(source.contains("putString(\"dungeon_tag\""),
				"The global living tick must never erase authoritative dungeon tags");
		expectTrue(source.contains("GateCompletionTokens.contains")
						&& source.contains("GateCompletionTokens.remove"),
				"Gate completion consumption must use exact comma-delimited tokens");
	}

	private static void returnPortalLifecycleMatchesGateType()
			throws IOException {
		String procedural = read("dungeon",
				"ProceduralDungeonGateHandler.java");
		expectTrue(procedural.contains(
						"targetPos, settingsFor(gate), currentFirst, false)"),
				"Normal procedural generation must defer its return portal");
		expectFalse(procedural.contains(
						"spawnUnscopedReturnPortal("),
				"Normal procedural entry must not create a return portal");

		String datapack = read("dungeon",
				"DatapackDungeonGateHandler.java");
		expectTrue(datapack.contains(
						"instance.setReturnPortalDeferred(true)"),
				"Normal datapack gates must defer their authored exit until the boss");
		String source = read("dungeon", "runtime",
				"SnowRedGateArenaManager.java");
		expectTrue(source.contains(
						"return open(world, gate, initiator, nearbyPartyMembers(world, gate, initiator), true,")
						&& source.contains(
								"return open(world, gate, initiator, entrants, false, territoryFor(gate));"),
				"Dedicated Red gates must defer their exit while procedural Red gates show the locked exit");
		expectTrue(source.contains(
						"instance.setReturnPortalDeferred(markLegacyUsed)"),
				"Only procedural Red Gates may create a pre-clear portal");
		int ensureStart = source.indexOf(
				"private static void ensureReturnPortal(");
		int ensureEnd = source.indexOf(
				"private enum PreparationPhase", ensureStart);
		expectTrue(ensureStart >= 0 && ensureEnd > ensureStart,
				"Red Gate return-portal lifecycle method must exist");
		String ensure = source.substring(ensureStart, ensureEnd);
		expectTrue(ensure.contains(
						"if (!instance.completed() && instance.returnPortalDeferred())")
						&& ensure.contains("\"Created locked\""),
				"Procedural Red Gate recovery must recreate its visible locked portal");
		String interaction = read("procedures",
				"DungeonDimensionPlayerLeavesDimensionProcedure.java");
		expectTrue(interaction.contains("if (!scopedCompletion)")
						&& interaction.contains(
								"You cant leave a red gate before you defeat the boss"),
				"The visible Red Gate return portal must refuse pre-boss exits");
		String abandoned = source.substring(
				source.indexOf("private static void failAbandonedInstance("),
				source.indexOf("private static void recordArenaClosure("));
		expectTrue(abandoned.contains(
						"discardReturnPortals(level, instance)"),
				"Abandoned Red Gate runs must not leave an exit for the next arena");
	}

	private static String read(String directory, String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory).resolve(file));
	}

	private static String read(String directory, String subdirectory,
			String file) throws IOException {
		return Files.readString(MAIN_SOURCE.resolve(directory)
				.resolve(subdirectory).resolve(file));
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}

	private static void expectEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual))
			throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
	}
}
