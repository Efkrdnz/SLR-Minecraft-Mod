package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Dependency-free rules and source contracts for Mage class styles. */
public final class ClassStyleEvaluationRegression {
	private static final Path MAIN = Path.of(
			"src", "main", "java", "net", "solocraft");

	private ClassStyleEvaluationRegression() {
	}

	public static void main(String[] args) throws IOException {
		onlyMageStylesAreEnabled();
		contributedStylesPlugInWithoutDisturbingBuiltIns();
		everyClassDeclaresItsFullStyleRoster();
		gatedStylesResolveButCannotBeDrawn();
		evaluationPresentationIsClassGeneric();
		mageStyleIdsRoundTrip();
		mageStyleBagIsCompleteAndNonRepeating();
		newWireIdsPreservePreStyleOrdinals();
		mageCeremonyAndRerollTimingIsExplicit();
		evaluatorOwnsPendingStyleAndRerolls();
		acceptedStylePrecedesMageRewardGrant();
		playerStateMigratesAndMirrorsTheMageAlias();
		fireMageReceivesAnERankEvaluationSpell();
		clientPresentsButCannotSubmitAStyleResult();
	}

	/**
	 * Assassin and Mage are the shipped rosters. The other four classes stay
	 * gated until their ability trees exist, so a player can never draw a style
	 * whose abilities do nothing.
	 */
	private static void onlyMageStylesAreEnabled() {
		for (int classId = 1; classId <= HunterEvaluationRules.CLASS_COUNT;
				classId++) {
			boolean expected = classId == ClassStyleRules.MAGE_CLASS_ID
					|| classId == ClassStyleRules.ASSASSIN_CLASS_ID
					|| classId == ClassStyleRules.FIGHTER_CLASS_ID
					|| classId == ClassStyleRules.HEALER_CLASS_ID
					|| classId == ClassStyleRules.TANKER_CLASS_ID;
			expectEquals(expected, ClassStyleRules.supportsStyles(classId),
					"Unexpected playable style roster for class " + classId);
		}
		expectEquals(0b11, ClassStyleRules.allStylesMask(
						ClassStyleRules.ASSASSIN_CLASS_ID),
				"Both Assassin styles must be drawable");
		expectEquals(0b111, ClassStyleRules.allStylesMask(
						ClassStyleRules.FIGHTER_CLASS_ID),
				"All three Fighter styles must be drawable");
		expectEquals(ClassStyleRules.ALL_MAGE_STYLES_MASK,
				ClassStyleRules.allStylesMask(ClassStyleRules.MAGE_CLASS_ID),
				"Mage style mask is incomplete");
		expectEquals(0b11, ClassStyleRules.allStylesMask(
						ClassStyleRules.HEALER_CLASS_ID),
				"Both Healer styles must be drawable");
		expectEquals(0b11, ClassStyleRules.allStylesMask(
						ClassStyleRules.TANKER_CLASS_ID),
				"Both Tanker styles must be drawable");
		expectEquals(0, ClassStyleRules.allStylesMask(ClassStyleRules.RANGER_CLASS_ID),
				"Ranger must stay gated until its abilities exist");
	}

	/**
	 * The registry declares every style each class will own, so enabling one
	 * later is a single flag flip rather than a schema change.
	 */
	private static void everyClassDeclaresItsFullStyleRoster() {
		// Assassin 2, Mage 5 (Fire/Barrier/Arcane/Storm/Curse), Fighter 3,
		// Tanker 2, Healer 2, Ranger 3.
		int[] expectedCounts = { 0, 2, 5, 3, 2, 2, 3 };
		Set<String> seenKeys = new HashSet<>();
		for (int classId = 1; classId <= HunterEvaluationRules.CLASS_COUNT;
				classId++) {
			var styles = ClassStyleRules.registeredStyles(classId);
			expectEquals(expectedCounts[classId], styles.size(),
					"Class " + classId + " must declare its full style roster");
			expectTrue(styles.size() >= 2,
					"Every class needs at least two styles");
			Set<Integer> seenIds = new HashSet<>();
			for (var style : styles) {
				expectTrue(seenIds.add(style.styleId()),
						"Style ids must be unique inside class " + classId);
				expectTrue(style.styleId() >= 1,
						"Style ids are one-based so they can form a bitmask");
				expectTrue(seenKeys.add(classId + ":" + style.key()),
						"Style keys must be unique inside their class");
				expectTrue(!style.key().isBlank() && !style.displayName().isBlank(),
						"Every style needs a saved key and a display name");
				expectTrue(style.key().equals(style.key().toLowerCase()),
						"Style keys must be lowercase snake_case");
			}
		}
	}

	/**
	 * A gated style must be invisible to the draw and commit paths while still
	 * resolving its name and colour, so an admin-granted or migrated value never
	 * renders as "Unresolved".
	 */
	private static void gatedStylesResolveButCannotBeDrawn() {
		// Ranger's roster is declared but gated; it must resolve for display
		// while staying invisible to the draw and commit paths.
		int ranger = ClassStyleRules.RANGER_CLASS_ID;
		expectEquals("Bowmaster", ClassStyleRules.styleName(ranger, 1),
				"A gated style must still resolve its display name");
		expectTrue(!ClassStyleRules.isValidStyle(ranger, 1),
				"A gated style must not be committable");
		expectEquals("", ClassStyleRules.styleKey(ranger, 1),
				"A gated style must not expose a saved key");
		expectEquals(0, ClassStyleRules.styleId(ranger, "bow"),
				"A gated key must not resolve to a drawable id");
		expectEquals(0, ClassStyleRules.drawStyle(ranger, 0, 0, 0).styleId(),
				"A class with no enabled styles must draw nothing");

		// Both shipped rosters must draw.
		int assassin = ClassStyleRules.ASSASSIN_CLASS_ID;
		expectTrue(ClassStyleRules.drawStyle(assassin, 0b11, 0, 0).styleId() > 0,
				"The Assassin roster must draw");
		expectEquals(ClassStyleRules.INFILTRATOR,
				ClassStyleRules.styleId(assassin, "infiltration"),
				"Infiltrator key must resolve now that it ships");
		expectEquals(ClassStyleRules.CUTTHROAT,
				ClassStyleRules.styleId(assassin, "assault"),
				"Cutthroat key must resolve now that it ships");
		expectTrue(ClassStyleRules.drawStyle(ClassStyleRules.MAGE_CLASS_ID,
						ClassStyleRules.ALL_MAGE_STYLES_MASK, 0, 0).styleId() > 0,
				"An enabled roster must still draw");

		// A reroll must never immediately repeat inside a two-style class.
		for (int current = 1; current <= 2; current++) {
			int drawn = ClassStyleRules.drawStyle(assassin, 0b11, current, 0).styleId();
			expectTrue(drawn != current,
					"An Assassin style reroll must not repeat the current style");
		}
	}

	/**
	 * The ceremony and the Hunter ID must describe whichever class is being
	 * evaluated. Mage-specific copy shipped for a year because Mage was the only
	 * styled class; this keeps the next roster from inheriting the same trap.
	 */
	private static void evaluationPresentationIsClassGeneric() throws IOException {
		String screen = readMain("client", "gui", "system",
				"HunterEvaluationScreen.java");
		for (String mageOnly : new String[] {
				"\"MANA ASPECT DETECTED\"",
				"\"RECALIBRATING MAGE STYLE\"",
				"\"STYLE  ANALYZING MANA ASPECT\"",
				"Mage style may be rerolled",
				"remaining Mage styles",
				"Resolving the Mage" }) {
			expectTrue(!screen.contains(mageOnly),
					"Evaluation copy must not hardcode Mage: " + mageOnly);
		}
		expectTrue(screen.contains("styleNoun(") && screen.contains("styleOwnerLabel("),
				"Evaluation copy must derive its wording from the class");

		String hunterId = readMain("item", "HunterIDItem.java");
		expectTrue(hunterId.contains("ClassStyleRules.styleName(classId, styleId)"),
				"Hunter ID must resolve the style name generically");
		expectTrue(!hunterId.contains("\"§9Assassin\""),
				"Hunter ID must not hardcode a styleless Assassin label");
	}

	private static void mageStyleIdsRoundTrip() {
		String[] expected = { "fire", "barrier", "arcane", "storm" };
		for (int styleId = 1; styleId <= expected.length; styleId++) {
			String key = ClassStyleRules.styleKey(
					ClassStyleRules.MAGE_CLASS_ID, styleId);
			expectEquals(expected[styleId - 1], key,
					"Unexpected Mage style key");
			expectEquals(styleId, ClassStyleRules.styleId(
					ClassStyleRules.MAGE_CLASS_ID, key),
					"Mage style key did not round-trip");
			expectTrue(!ClassStyleRules.styleName(
					ClassStyleRules.MAGE_CLASS_ID, styleId)
					.equals("Unresolved"), "Mage style has no display name");
		}
	}

	private static void mageStyleBagIsCompleteAndNonRepeating() {
		int mask = ClassStyleRules.ALL_MAGE_STYLES_MASK;
		int current = 0;
		Set<Integer> firstBag = new HashSet<>();
		for (int drawIndex = 0;
				drawIndex < ClassStyleRules.MAGE_STYLE_COUNT; drawIndex++) {
			ClassStyleRules.StyleDraw draw = ClassStyleRules.drawStyle(
					ClassStyleRules.MAGE_CLASS_ID, mask, current, drawIndex);
			expectTrue(draw.styleId() != current,
					"Mage style reroll repeated the current result");
			firstBag.add(draw.styleId());
			current = draw.styleId();
			mask = draw.remainingMask();
		}
		expectEquals(ClassStyleRules.MAGE_STYLE_COUNT, firstBag.size(),
				"Mage style bag did not contain every style exactly once");
		expectEquals(0, mask, "Mage style bag did not exhaust");

		int previous = current;
		ClassStyleRules.StyleDraw refill = ClassStyleRules.drawStyle(
				ClassStyleRules.MAGE_CLASS_ID, mask, current, 0);
		expectTrue(refill.styleId() != previous,
				"Refilled Mage style bag repeated its boundary result");
	}

	private static void newWireIdsPreservePreStyleOrdinals() {
		expectEquals(9, HunterEvaluationRules.Phase.STYLE_REVEAL.ordinal(),
				"STYLE_REVEAL must be appended after pre-style phases");
		expectEquals(10, HunterEvaluationRules.Phase.REROLL_STYLE.ordinal(),
				"REROLL_STYLE phase must be append-only");
		expectEquals(5, HunterEvaluationRules.Action.REROLL_STYLE.ordinal(),
				"REROLL_STYLE action must preserve old action IDs");
		expectEquals(30, HunterEvaluationRules.phaseDuration(
				HunterEvaluationRules.Mode.INITIAL,
				HunterEvaluationRules.Phase.STYLE_REVEAL),
				"Mage style reveal timing changed unexpectedly");
	}

	private static void mageCeremonyAndRerollTimingIsExplicit() {
		int baseCeremony = HunterEvaluationRules.CONTACT_TICKS
				+ HunterEvaluationRules.INITIAL_BOOT_TICKS
				+ HunterEvaluationRules.INITIAL_SCAN_TICKS
				+ HunterEvaluationRules.INITIAL_CLASS_REVEAL_TICKS
				+ HunterEvaluationRules.RANK_REVEAL_TICKS
				+ HunterEvaluationRules.INITIAL_SETTLE_TICKS;
		expectEquals(220, baseCeremony,
				"The existing non-Mage evaluation ceremony changed");
		expectEquals(250,
				baseCeremony + HunterEvaluationRules.STYLE_REVEAL_TICKS,
				"Mage evaluation must include exactly one style reveal");
		expectEquals(20, HunterEvaluationRules.phaseDuration(
				HunterEvaluationRules.Mode.INITIAL,
				HunterEvaluationRules.Phase.REROLL_STYLE),
				"Mage style reroll timing changed unexpectedly");
	}

	private static void evaluatorOwnsPendingStyleAndRerolls()
			throws IOException {
		String manager = readMain("util", "HunterEvaluationManager.java");
		expectTrue(manager.contains("case REROLL_STYLE -> rerollStyle")
				&& manager.contains("RemainingStyleMask")
				&& manager.contains("drawStyleForCurrentClass")
				&& manager.contains("Phase.STYLE_REVEAL")
				&& manager.contains("Phase.REROLL_STYLE"),
				"Evaluator session does not own Mage style draw/reroll state");
		expectTrue(manager.contains(
				"ClassStyleRules.supportsStyles(session.classId)"),
				"Non-Mage evaluations must skip style phases");
	}

	private static void acceptedStylePrecedesMageRewardGrant()
			throws IOException {
		String manager = readMain("util", "HunterEvaluationManager.java");
		int commit = manager.indexOf("private static void commitInitial");
		int next = manager.indexOf("private static void commitReevaluation",
				commit);
		expectTrue(commit >= 0 && next > commit,
				"Could not isolate initial evaluation commit");
		String body = manager.substring(commit, next);
		int genericStyle = body.indexOf(
				"capability.classStyle = selectedStyle");
		int legacyAlias = body.indexOf(
				"capability.mageSpecialization = selectedStyle");
		int rewards = body.indexOf(
				"HunterEvaluationRewardService.applyInitialRewards");
		expectTrue(genericStyle >= 0 && legacyAlias > genericStyle
				&& rewards > legacyAlias,
				"Accepted Mage style must be persisted before spell rewards run");
	}

	private static void playerStateMigratesAndMirrorsTheMageAlias()
			throws IOException {
		String variables = readMain("network",
				"SololevelingModVariables.java");
		expectTrue(variables.contains("public String classStyle = \"\"")
				&& variables.contains("clone.classStyle = original.classStyle")
				&& variables.contains("nbt.putString(\"classStyle\"")
				&& variables.contains("classStyle = nbt.getString(\"classStyle\")")
				&& variables.contains(
						"variables.classStyle = message.data.classStyle"),
				"Generic class style is not fully saved, cloned and synchronized");

		String mage = readMain("util", "MageSpellProgression.java");
		expectTrue(mage.contains("capability.classStyle = selected")
				&& mage.contains("capability.mageSpecialization = selected")
				&& mage.contains("String generic = normalizeStyle(vars.classStyle)"),
				"Mage progression must prefer generic style and mirror its old alias");
	}

	private static void fireMageReceivesAnERankEvaluationSpell()
			throws IOException {
		String mage = readMain("util", "MageSpellProgression.java");
		expectTrue(mage.contains(
				"List.of(FireMageSpellManager.FLAME_WEAVING)")
				&& mage.contains(
						"for (int currentTier = 0; currentTier <= tier; currentTier++)"),
				"E-Rank Fire Mages must receive Flame Weaving at evaluation");
	}

	private static void clientPresentsButCannotSubmitAStyleResult()
			throws IOException {
		String screen = readMain("client", "gui", "system",
				"HunterEvaluationScreen.java");
		expectTrue(screen.contains("REROLL STYLE")
				&& screen.contains("STYLE_STAGE_NAMES")
				&& screen.contains("ClassStyleRules.styleDescription"),
				"Evaluator screen does not present Mage style reveal/reroll UI");

		String action = readMain("network",
				"HunterEvaluationActionMessage.java");
		expectTrue(action.contains("private final UUID sessionId")
				&& action.contains("private final int actionId"),
				"Client action packet shape changed unexpectedly");
		expectFalse(action.contains("styleId")
				|| action.contains("styleKey")
				|| action.contains("classStyle"),
				"Client must never submit a trusted style result");
	}

	/**
	 * A contributed class can offer styles, and doing so must not touch a shipped
	 * class's roster.
	 *
	 * <p>The hook is an installed {@code IntFunction} rather than a direct call
	 * so this class keeps loading without Minecraft. That is exactly what lets
	 * this test run, so it is worth pinning: an accidental direct reference to
	 * the contributed registry would break the whole suite with a
	 * NoClassDefFoundError rather than a readable failure.
	 */
	private static void contributedStylesPlugInWithoutDisturbingBuiltIns() {
		int mageBefore = ClassStyleRules.allStylesMask(ClassStyleRules.MAGE_CLASS_ID);
		int contributedClassId = 40;

		expectEquals(0, ClassStyleRules.allStylesMask(contributedClassId),
				"A class with no contributed source must offer no styles");

		try {
			ClassStyleRules.installContributedSource(classId -> classId != contributedClassId
					? java.util.List.of()
					: java.util.List.of(
							new ClassStyleRules.Style(classId, 1, "gravebound", "Gravebound",
									"Keeps what it raises", 0xFF8FE3B0, true),
							new ClassStyleRules.Style(classId, 2, "cairnwarden", "Cairnwarden",
									"Holds the line it dug", 0xFF6FC0A0, true)));

			expectEquals(0b11, ClassStyleRules.allStylesMask(contributedClassId),
					"Both contributed styles must be drawable");
			expectTrue(ClassStyleRules.supportsStyles(contributedClassId),
					"A contributed class with styles must report that it has them");
			expectEquals("Gravebound", ClassStyleRules.styleName(contributedClassId, 1),
					"A contributed style must resolve to its own name");
			expectEquals(2, ClassStyleRules.styleId(contributedClassId, "cairnwarden"),
					"A contributed style key must round-trip to its id");
			expectEquals("cairnwarden", ClassStyleRules.styleKey(contributedClassId, 2),
					"The persisted key, not the number, is what identifies a style");

			// The bag must behave the same as a built-in class's: a draw excludes
			// the current style so a reroll never repeats what is on screen.
			ClassStyleRules.StyleDraw draw = ClassStyleRules.drawStyle(
					contributedClassId, 0b11, 1, 0);
			expectEquals(2, draw.styleId(),
					"A reroll away from style 1 must land on the other style");

			expectEquals(mageBefore, ClassStyleRules.allStylesMask(ClassStyleRules.MAGE_CLASS_ID),
					"A contributed source must not change a shipped class's roster");
		} finally {
			ClassStyleRules.installContributedSource(null);
		}

		expectEquals(0, ClassStyleRules.allStylesMask(contributedClassId),
				"Clearing the source must leave no contributed styles behind");
		expectEquals(mageBefore, ClassStyleRules.allStylesMask(ClassStyleRules.MAGE_CLASS_ID),
				"The shipped roster must survive the source being cleared");
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

	private static void expectEquals(String expected, String actual,
			String message) {
		if (!expected.equals(actual))
			throw new AssertionError(message + ": expected " + expected
					+ ", got " + actual);
	}

	private static void expectEquals(boolean expected, boolean actual,
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
