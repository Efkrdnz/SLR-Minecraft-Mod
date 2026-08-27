package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Dependency-free rules and source contracts for Hunter Evaluation. */
public final class HunterEvaluationRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");
	private static final Path SHADERS = Path.of("src", "main", "resources",
			"assets", "sololeveling", "shaders", "core");

	private HunterEvaluationRegression() {
	}

	public static void main(String[] args) throws IOException {
		weightedRankOddsMatchLegacyBalance();
		classRerollsUseACompleteNonRepeatingShuffle();
		ceremonyPacingMatchesTheApprovedTimings();
		clientCannotSubmitClassRankOrCoordinates();
		sessionsAndRewardsHaveDurableServerReceipts();
		legacySecretaryAndTickTimerAreDetached();
		evaluatorOwnsNormalClassAssignment();
		hunterIdUsesCertifiedRank();
		shaderAndFallbackArePackaged();
		contactOnlyAdvancesWhileHeld();
		awakeningOddsRescaleToOneHundred();
		sRankRevealReportsAnErrorFirst();
		levelProgressionUsesStableRankFloors();
		vesselsAlwaysEvaluateAsSRank();
		firstEvaluationUsesVesselAndLevelAuthority();
		managerUsesAuthoritativeProgressionRules();
	}

	private static void levelProgressionUsesStableRankFloors() {
		int[][] boundaries = {
				{ 0, 1 }, { 14, 1 },
				{ 15, 2 }, { 29, 2 },
				{ 30, 3 }, { 49, 3 },
				{ 50, 4 }, { 74, 4 },
				{ 75, 5 }, { 99, 5 },
				{ 100, 6 }, { 250, 6 }
		};
		for (int[] boundary : boundaries)
			expectEquals(boundary[1],
					HunterEvaluationRules.rankFloorForLevel(boundary[0]),
					"Unexpected level rank floor at level " + boundary[0]);

		int previous = 0;
		for (int level = 0; level <= 250; level++) {
			int rank = HunterEvaluationRules.rankFloorForLevel(level);
			expectTrue(rank >= previous,
					"Level rank floor went backwards at level " + level);
			previous = rank;
		}

		expectEquals(5, HunterEvaluationRules.resolvedEvaluationRank(
				5, 2, 30, false),
				"A strong initial evaluation must never be downgraded by level");
		expectEquals(4, HunterEvaluationRules.resolvedEvaluationRank(
				2, 3, 50, false),
				"Level 50 must promote an ordinary hunter to at least B-Rank");
		expectEquals(5, HunterEvaluationRules.resolvedEvaluationRank(
				2, 5, 15, false),
				"Reevaluation must preserve a higher certified rank");
	}

	private static void vesselsAlwaysEvaluateAsSRank() {
		for (int level : new int[] { 0, 1, 15, 30, 50, 70, 100, 250 }) {
			for (int earned = 0; earned <= 6; earned++) {
				expectEquals(6,
						HunterEvaluationRules.resolvedEvaluationRank(
								earned, 0, level, true),
						"A vessel did not evaluate as S-Rank at level "
								+ level);
			}
		}
	}

	private static void firstEvaluationUsesVesselAndLevelAuthority()
			throws IOException {
		for (int rolledRank = 1; rolledRank <= 6; rolledRank++)
			expectEquals(6, HunterEvaluationRules.resolvedEvaluationRank(
					rolledRank, 0, 1, true),
					"A first-time vessel evaluation must override roll "
							+ rolledRank + " with S-Rank");
		expectEquals(5, HunterEvaluationRules.resolvedEvaluationRank(
				1, 0, 75, false),
				"A delayed first evaluation at level 75 must be at least A-Rank");
		expectEquals(6, HunterEvaluationRules.resolvedEvaluationRank(
				1, 0, 100, false),
				"A delayed first evaluation at level 100 must be S-Rank");
		expectEquals(6, HunterEvaluationRules.resolvedEvaluationRank(
				6, 0, 1, false),
				"A lucky S-Rank first roll must survive a low level floor");

		String manager = readMain("util", "HunterEvaluationManager.java");
		int createStart = manager.indexOf(
				"private static Session createSession");
		int createEnd = manager.indexOf(
				"private static boolean beginContact", createStart);
		expectTrue(createStart >= 0 && createEnd > createStart,
				"Could not isolate first-evaluation session creation");
		String create = manager.substring(createStart, createEnd);
		int roll = create.indexOf("HunterRankOdds.roll");
		int resolve = create.indexOf(
				"int resultRank = resolvedEvaluationRank(player, variables,");
		int initialSession = create.indexOf("Session.create(Mode.INITIAL");
		expectTrue(roll >= 0 && resolve > roll
					&& create.indexOf("candidateRank", resolve) > resolve
					&& initialSession > resolve,
				"First evaluation must resolve its roll before creating the initial session");

		int commitStart = manager.indexOf(
				"private static void commitInitial");
		int commitEnd = manager.indexOf(
				"private static void commitReevaluation", commitStart);
		expectTrue(commitStart >= 0 && commitEnd > commitStart,
				"Could not isolate first-evaluation commit");
		String commit = manager.substring(commitStart, commitEnd);
		expectTrue(commit.contains(
					"resolvedEvaluationRank(player, variables(player)")
					&& commit.contains("capability.HunterRank = resultRank")
					&& commit.contains("capability.prevRank = resultRank")
					&& commit.contains("applyInitialRewards(player,")
					&& commit.contains("selectedClass, resultRank"),
				"Accepted first evaluations must recheck and persist the authoritative rank");
	}

	private static void managerUsesAuthoritativeProgressionRules()
			throws IOException {
		String manager = readMain("util", "HunterEvaluationManager.java");
		expectTrue(manager.contains(
					"VesselManager.currentDefinition(player) != null"),
				"Evaluation must recognize explicit and legacy vessel assignments");
		expectTrue(manager.contains(
					"HunterEvaluationRules.resolvedEvaluationRank"),
				"Evaluation sessions must use the shared rank resolver");
		expectTrue(manager.contains("capability.HunterRank = result")
					&& manager.contains("capability.prevRank = result")
					&& manager.contains("capability.prevLevel = capability.Level"),
				"Reevaluation must synchronize earned rank, certified rank, and level");

		String levelUp = readMain("procedures", "LevelUpProcedure.java");
		expectTrue(levelUp.contains(
					"HunterEvaluationRules.rankFloorForLevel(newLevel)"),
				"Normal leveling must use the same rank floor as reevaluation");
		expectTrue(levelUp.contains("if (initialRank > 0.0D)"),
				"Leveling must not consume the first-evaluation roll for unranked players");
		expectFalse(levelUp.contains("newLevel / 25")
					|| levelUp.contains("initialLevel / 25"),
				"Rank progression must not remain relative to the initial roll");

		String gates = readMain("util", "GateSpawnerUtil.java");
		expectTrue(gates.contains("HunterEvaluationRules.rankFloorForLevel(")
					&& gates.contains("Math.max(certifiedRank, levelFloor)")
					&& gates.contains("VesselManager.currentDefinition(target) != null"),
				"Dungeon progression must share the evaluation level floor, preserve certification, and recognize vessels");
	}

	/**
	 * Configured odds are weights, not literal percentages, so any total is
	 * rescaled proportionally and always lands on exactly 100.
	 */
	private static void awakeningOddsRescaleToOneHundred() {
		int[] defaults = HunterEvaluationRules.normalizedRankOdds(
				HunterEvaluationRules.DEFAULT_RANK_ODDS);
		for (int rank = 1; rank <= 6; rank++)
			expectEquals(HunterEvaluationRules.DEFAULT_RANK_ODDS[rank - 1],
					defaults[rank - 1],
					"Default odds must pass through unchanged for rank " + rank);

		// The documented example: weights totalling 110 with S at 11 give S = 10%.
		int[] overweight = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 25, 25, 25, 12, 12, 11 });
		expectEquals(100, sum(overweight),
				"Rescaled odds must total exactly 100");
		expectEquals(10, overweight[5],
				"An S weight of 11 out of 110 must become a 10% chance");

		int[] underweight = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 5, 5, 5, 5, 5, 5 });
		expectEquals(100, sum(underweight),
				"Odds below 100 must still be scaled up to total 100");
		int[] lopsided = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 1, 1, 1, 1, 1, 100 });
		expectEquals(100, sum(lopsided),
				"Lopsided odds must still total 100");
		expectTrue(lopsided[5] > 90,
				"A dominant S weight must dominate the resulting odds");

		int[] zeroed = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 0, 0, 0, 0, 0, 0 });
		expectEquals(100, sum(zeroed), "All-zero odds must fall back to defaults");
		expectEquals(HunterEvaluationRules.DEFAULT_RANK_ODDS[5], zeroed[5],
				"All-zero odds must fall back to the default S chance");

		// A rank set to zero must be unreachable, and every roll must resolve.
		int[] noS = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 25, 25, 25, 12, 13, 0 });
		for (int roll = 1; roll <= 100; roll++) {
			int rank = HunterEvaluationRules.weightedRank(noS, roll);
			expectTrue(rank >= 1 && rank <= 6, "Roll " + roll + " left the rank range");
			expectTrue(rank != 6, "A zero S weight must never award S rank");
		}

		// Drawn frequencies must match the configured percentages exactly.
		int[] odds = HunterEvaluationRules.normalizedRankOdds(
				new int[] { 30, 20, 20, 15, 10, 5 });
		int[] counts = new int[7];
		for (int roll = 1; roll <= 100; roll++)
			counts[HunterEvaluationRules.weightedRank(odds, roll)]++;
		for (int rank = 1; rank <= 6; rank++)
			expectEquals(odds[rank - 1], counts[rank],
					"Configured odds must be drawn at their stated rate for rank "
							+ rank);
	}

	/** S rank must fault before it resolves, and only S may take the extra time. */
	private static void sRankRevealReportsAnErrorFirst() throws IOException {
		expectEquals(HunterEvaluationRules.RANK_REVEAL_TICKS,
				HunterEvaluationRules.rankRevealDuration(5),
				"Only an S rank reveal may run long");
		expectEquals(HunterEvaluationRules.RANK_REVEAL_TICKS
						+ HunterEvaluationRules.S_RANK_ERROR_TICKS,
				HunterEvaluationRules.rankRevealDuration(6),
				"An S rank reveal must reserve time for the error report");
		float share = HunterEvaluationRules.sRankErrorFraction();
		expectTrue(share > 0.0F && share < 1.0F,
				"The error window must be part of the S rank reveal, not all of it");

		String screen = readMain("client", "gui", "system",
				"HunterEvaluationScreen.java");
		expectTrue(screen.contains("isSRankError()")
						&& screen.contains("sRankErrorFraction()"),
				"The screen must gate its error report on the shared timing");
		String manager = readMain("util", "HunterEvaluationManager.java");
		expectTrue(manager.contains("rankRevealDuration(session.rank)"),
				"The server must lengthen the reveal for an S rank");
	}

	private static int sum(int[] values) {
		int total = 0;
		for (int value : values)
			total += value;
		return total;
	}

	/**
	 * Contact must never arm itself. Regressed once because startPhase applied
	 * the rules table to every phase, so CONTACT began a 30 tick countdown as
	 * soon as BOOT ended and the ceremony completed without the player holding.
	 */
	private static void contactOnlyAdvancesWhileHeld() throws IOException {
		String manager = readMain("util", "HunterEvaluationManager.java");
		expectTrue(manager.contains("phase == Phase.CONTACT ? 0"),
				"startPhase must enter CONTACT unarmed so a held press is required");
		expectTrue(manager.contains(
						"session.deadline = now + HunterEvaluationRules.CONTACT_TICKS"),
				"beginContact must be what arms the contact countdown");
		String screen = readMain("client", "gui", "system",
				"HunterEvaluationScreen.java");
		expectTrue(screen.contains("sendImmediate(Action.BEGIN_CONTACT)")
						&& screen.contains("sendImmediate(Action.CANCEL_CONTACT)"),
				"Press and release must both reach the server unguarded");
	}

	private static void weightedRankOddsMatchLegacyBalance() {
		int[] counts = new int[7];
		for (int roll = 1; roll <= 100; roll++)
			counts[HunterEvaluationRules.weightedRank(roll)]++;
		int[] expected = { 0, 25, 25, 25, 12, 10, 3 };
		for (int rank = 1; rank <= 6; rank++)
			expectEquals(expected[rank], counts[rank],
					"Unexpected weighted odds for rank " + rank);
	}

	private static void classRerollsUseACompleteNonRepeatingShuffle() {
		int mask = HunterEvaluationRules.ALL_CLASSES_MASK;
		int current = 0;
		Set<Integer> firstBag = new HashSet<>();
		for (int drawIndex = 0; drawIndex < 6; drawIndex++) {
			HunterEvaluationRules.ClassDraw draw =
					HunterEvaluationRules.drawClass(mask, current, 0);
			expectTrue(draw.classId() != current,
					"A reroll returned the current class");
			firstBag.add(draw.classId());
			current = draw.classId();
			mask = draw.remainingMask();
		}
		expectEquals(6, firstBag.size(),
				"The first shuffle did not show all six classes once");
		expectEquals(0, mask, "The first shuffle bag was not exhausted");

		Set<Integer> secondBag = new HashSet<>();
		for (int drawIndex = 0; drawIndex < 6; drawIndex++) {
			int previous = current;
			HunterEvaluationRules.ClassDraw draw =
					HunterEvaluationRules.drawClass(mask, current, drawIndex);
			expectTrue(draw.classId() != previous,
					"A refill repeated the boundary class immediately");
			secondBag.add(draw.classId());
			current = draw.classId();
			mask = draw.remainingMask();
		}
		expectEquals(6, secondBag.size(),
				"The refilled shuffle did not contain all six classes");
	}

	private static void ceremonyPacingMatchesTheApprovedTimings() {
		int initial = HunterEvaluationRules.INITIAL_BOOT_TICKS
				+ HunterEvaluationRules.CONTACT_TICKS
				+ HunterEvaluationRules.INITIAL_SCAN_TICKS
				+ HunterEvaluationRules.INITIAL_CLASS_REVEAL_TICKS
				+ HunterEvaluationRules.RANK_REVEAL_TICKS
				+ HunterEvaluationRules.INITIAL_SETTLE_TICKS;
		int reevaluation = HunterEvaluationRules.CONTACT_TICKS
				+ HunterEvaluationRules.REEVALUATION_SCAN_TICKS
				+ HunterEvaluationRules.RANK_REVEAL_TICKS;
		expectEquals(220, initial,
				"Initial Evaluation must last exactly 11 seconds");
		expectEquals(100, reevaluation,
				"Reevaluation must last exactly 5 seconds");
		expectTrue(HunterEvaluationRules.rankIntensity(1)
						< HunterEvaluationRules.rankIntensity(5)
						&& HunterEvaluationRules.rankIntensity(5)
								< HunterEvaluationRules.rankIntensity(6),
				"Rank brightness must rise from E through S");
	}

	private static void clientCannotSubmitClassRankOrCoordinates()
			throws IOException {
		String action = readMain("network",
				"HunterEvaluationActionMessage.java");
		expectTrue(action.contains("private final UUID sessionId")
						&& action.contains("private final int actionId"),
				"Evaluation actions need only a session and action ID");
		expectFalse(action.contains("classId")
						|| action.contains("rankResult")
						|| action.contains("BlockPos"),
				"The client packet must not submit a class, rank, or evaluator position");

		String manager = readMain("util", "HunterEvaluationManager.java");
		expectTrue(manager.contains("!session.id.equals(sessionId)")
						&& manager.contains("!isNearStation(player, session)")
						&& manager.contains("MAX_DISTANCE_SQUARED")
						&& manager.contains("isEvaluator("),
				"Every action must validate session, distance, and evaluator block");
	}

	private static void sessionsAndRewardsHaveDurableServerReceipts()
			throws IOException {
		String manager = readMain("util", "HunterEvaluationManager.java");
		String rewards = readMain("util",
				"HunterEvaluationRewardService.java");
		expectTrue(manager.contains("Player.PERSISTED_NBT_TAG")
						&& manager.contains("RemainingClassMask")
						&& manager.contains("PausedTicks")
						&& manager.contains("LEGACY_MIGRATED"),
				"Pending sessions and migration need death-safe persistence");
		int receipt = rewards.indexOf(
				"evaluationData.putBoolean(REWARDS_APPLIED, true)");
		int additiveStats = rewards.indexOf(
				"applyStats(player, classId, rank)");
		expectTrue(receipt >= 0 && additiveStats > receipt,
				"The one-shot receipt must precede additive reward mutations");
		expectTrue(manager.contains("Mode.REEVALUATION")
						&& manager.contains("resolvedEvaluationRank(player")
						&& manager.contains("HunterIDItem.refreshAll(player)"),
				"Reevaluation must certify earned progression and refresh IDs only");
	}

	private static void legacySecretaryAndTickTimerAreDetached()
			throws IOException {
		String secretary = readMain("entity", "SecretaryEntity.java");
		expectFalse(secretary.contains("DialogueProcedure"),
				"The Secretary must remain aesthetic only");
		String timer = readMain("procedures",
				"EvaluationTimerProcedure.java");
		int classDeclaration = timer.indexOf(
				"public final class EvaluationTimerProcedure");
		int playerTick = timer.indexOf("public static void onPlayerTick");
		expectTrue(classDeclaration >= 0 && playerTick > classDeclaration
						&& !timer.substring(classDeclaration, playerTick)
								.contains("@SubscribeEvent"),
				"The legacy 150-tick evaluation timer must not subscribe anymore");
		String stone = readMain("procedures",
				"RankEvaluatorOnBlockRightClickedProcedure.java");
		expectTrue(stone.contains(
						"HunterEvaluationManager.openEvaluator"),
				"The evaluator stone must be the sole ceremony entry point");
	}

	private static void evaluatorOwnsNormalClassAssignment()
			throws IOException {
		String awakening = readMain("util",
				"StatAwakeningManager.java");
		int tick = awakening.indexOf(
				"public static void onPlayerTick");
		int compute = awakening.indexOf(
				"private static int computeClass", tick);
		String tickBody = awakening.substring(tick, compute);
		expectFalse(tickBody.contains("triggerAwakening(")
						|| tickBody.contains("cap.Classes ="),
				"Normal level ticks must not assign a class");
		expectTrue(tickBody.contains("Hunter Evaluation Available")
						&& tickBody.contains("Evaluator crystal"),
				"Unclassified players should receive evaluator reminders");
	}

	private static void hunterIdUsesCertifiedRank() throws IOException {
		String card = readMain("item", "HunterIDItem.java");
		expectTrue(card.contains("variables.prevRank")
						&& card.contains("EvaluationOwner")
						&& card.contains("refreshAll(ServerPlayer player)"),
				"Hunter IDs must be owner-bound and show certified rank");
		expectFalse(card.contains("certifiedRank = bounded((int) Math.round(variables.HunterRank)"),
				"Hunter IDs must not expose an uncertified earned rank");
	}

	private static void shaderAndFallbackArePackaged() throws IOException {
		expectTrue(Files.isRegularFile(SHADERS.resolve(
						"hunter_evaluation_background.vsh"))
						&& Files.isRegularFile(SHADERS.resolve(
								"hunter_evaluation_background.fsh"))
						&& Files.isRegularFile(SHADERS.resolve(
								"rendertype_hunter_evaluation_background.json")),
				"Evaluation shader sources and definition are missing");
		String screen = readMain("client", "gui", "system",
				"HunterEvaluationScreen.java");
		expectTrue(screen.contains("renderFallback(graphics)")
						&& screen.contains("RankIntensity")
						&& screen.contains("PRESS AND HOLD THE GEM"),
				"The screen needs shader fallback, rank brightness, and hold guidance");
	}

	private static String readMain(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expectEquals(int expected, int actual,
			String message) {
		if (expected != actual)
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
