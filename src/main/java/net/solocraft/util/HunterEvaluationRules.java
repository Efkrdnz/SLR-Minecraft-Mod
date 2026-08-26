package net.solocraft.util;

import java.util.List;
import java.util.Optional;

import net.solocraft.api.hunter.HunterClass;
import net.solocraft.api.hunter.HunterClassPresentation;
import net.solocraft.api.hunter.HunterClassRegistry;

/**
 * Pure rules shared by the Hunter Evaluation server flow, client presentation,
 * and regression tests.
 */
public final class HunterEvaluationRules {
	public static final int CLASS_COUNT = 6;
	public static final int ALL_CLASSES_MASK = (1 << CLASS_COUNT) - 1;

	/**
	 * Contributed classes are drawn as ids above the built-in six.
	 *
	 * <p>The number is positional, not stored: it indexes the ordered list of
	 * rollable contributed classes, which a client and a server derive the same
	 * way. What reaches the save is the class identity, never this number. The
	 * mask being an int is what caps the total.
	 */
	public static final int MAX_CLASS_ID = 32;

	/** Every class the Evaluator may draw right now, built-in and contributed. */
	public static int drawableClassMask() {
		int mask = ALL_CLASSES_MASK;
		int contributed = HunterClassRegistry.evaluatorClasses().size();
		for (int index = 0; index < contributed; index++) {
			int classId = CLASS_COUNT + 1 + index;
			if (classId > MAX_CLASS_ID)
				break;
			mask |= 1 << (classId - 1);
		}
		return mask;
	}

	/** The contributed class a drawn id refers to, or empty for a built-in. */
	public static Optional<HunterClass> contributedClass(int classId) {
		int index = classId - CLASS_COUNT - 1;
		if (index < 0)
			return Optional.empty();
		List<HunterClass> classes = HunterClassRegistry.evaluatorClasses();
		return index < classes.size() ? Optional.of(classes.get(index)) : Optional.empty();
	}
	public static final int RANK_COUNT = 6;
	public static final int D_RANK_LEVEL = 15;
	public static final int C_RANK_LEVEL = 30;
	public static final int B_RANK_LEVEL = 50;
	public static final int A_RANK_LEVEL = 75;
	public static final int S_RANK_LEVEL = 100;

	public static final int CONTACT_TICKS = 30;
	public static final int INITIAL_BOOT_TICKS = 40;
	public static final int INITIAL_SCAN_TICKS = 70;
	public static final int INITIAL_CLASS_REVEAL_TICKS = 40;
	public static final int STYLE_REVEAL_TICKS = 30;
	public static final int RANK_REVEAL_TICKS = 30;
	public static final int INITIAL_SETTLE_TICKS = 10;
	public static final int REEVALUATION_SCAN_TICKS = 40;
	public static final int REROLL_TICKS = 25;
	public static final int REROLL_STYLE_TICKS = 20;
	/**
	 * Extra time granted only to an S-Rank reveal, during which the evaluator
	 * reports a measurement error before the real result resolves. It is added on
	 * top of {@link #RANK_REVEAL_TICKS}, so the approved ceremony totals for every
	 * other rank are unchanged.
	 */
	public static final int S_RANK_ERROR_TICKS = 35;

	/** Default awakening odds for E, D, C, B, A, S. Sums to 100. */
	public static final int[] DEFAULT_RANK_ODDS = { 25, 25, 25, 12, 10, 3 };
	public static final int MAX_RANK_WEIGHT = 100;

	private static final String[] CLASS_NAMES = {
			"Unknown", "Assassin", "Mage", "Fighter", "Tanker", "Healer", "Ranger"
	};
	private static final int[] CLASS_COLORS = {
			0xFF3FC6FF,
			0xFF009DFF,
			0xFFBF5FFF,
			0xFFD8D8D8,
			0xFF3060FF,
			0xFF06FF00,
			0xFFFFBC00
	};
	private static final String[] RANK_NAMES = {
			"Unranked", "E", "D", "C", "B", "A", "S"
	};
	private static final float[] RANK_INTENSITIES = {
			0.22F, 0.30F, 0.42F, 0.55F, 0.70F, 0.85F, 1.00F
	};

	private HunterEvaluationRules() {
	}

	/** Maps the original inclusive 1..100 evaluation roll to E..S. */
	public static int weightedRank(int roll) {
		int bounded = Math.max(1, Math.min(100, roll));
		if (bounded <= 25)
			return 1;
		if (bounded <= 50)
			return 2;
		if (bounded <= 75)
			return 3;
		if (bounded <= 87)
			return 4;
		if (bounded <= 97)
			return 5;
		return 6;
	}

	/**
	 * Minimum earned rank justified by System level. These boundaries mirror the
	 * dungeon progression ladder so a player's certification and the content they
	 * are prepared to enter no longer use competing level scales.
	 */
	public static int rankFloorForLevel(int level) {
		if (level >= S_RANK_LEVEL)
			return 6;
		if (level >= A_RANK_LEVEL)
			return 5;
		if (level >= B_RANK_LEVEL)
			return 4;
		if (level >= C_RANK_LEVEL)
			return 3;
		if (level >= D_RANK_LEVEL)
			return 2;
		return 1;
	}

	/**
	 * Resolves the server-authoritative evaluation result. A vessel is always
	 * measured as S-Rank; ordinary hunters keep the strongest of their innate or
	 * earned rank, previous certification, and current level floor. Reevaluation
	 * therefore only promotes and can never downgrade a player.
	 */
	public static int resolvedEvaluationRank(int earnedRank, int certifiedRank,
			int level, boolean hasVessel) {
		if (hasVessel)
			return RANK_COUNT;
		return Math.max(rankFloorForLevel(level),
				Math.max(clamp(earnedRank, 0, RANK_COUNT),
						clamp(certifiedRank, 0, RANK_COUNT)));
	}

	/**
	 * Rescales arbitrary per-rank weights into percentages that total exactly 100.
	 *
	 * <p>Weights are treated as proportions, not as literal percentages, so a
	 * configuration summing to 110 with an S weight of 11 yields an S chance of
	 * 10%. Fractional leftovers are handed out by the largest-remainder method so
	 * the result always sums to 100 without favouring any particular rank. Empty
	 * or all-zero input falls back to {@link #DEFAULT_RANK_ODDS}.
	 */
	public static int[] normalizedRankOdds(int[] weights) {
		int[] source = new int[RANK_COUNT];
		long total = 0L;
		for (int index = 0; index < RANK_COUNT; index++) {
			int weight = weights != null && index < weights.length
					? weights[index] : 0;
			source[index] = Math.max(0, Math.min(MAX_RANK_WEIGHT, weight));
			total += source[index];
		}
		if (total <= 0L)
			return DEFAULT_RANK_ODDS.clone();

		int[] result = new int[RANK_COUNT];
		int assigned = 0;
		for (int index = 0; index < RANK_COUNT; index++) {
			result[index] = (int) (source[index] * 100L / total);
			assigned += result[index];
		}
		// At most five points can remain, so one pass over the ranks suffices.
		boolean[] raised = new boolean[RANK_COUNT];
		for (int remaining = 100 - assigned; remaining > 0; remaining--) {
			int best = -1;
			long bestRemainder = -1L;
			for (int index = 0; index < RANK_COUNT; index++) {
				if (raised[index])
					continue;
				long remainder = source[index] * 100L % total;
				if (remainder > bestRemainder) {
					bestRemainder = remainder;
					best = index;
				}
			}
			if (best < 0)
				break;
			result[best]++;
			raised[best] = true;
		}
		return result;
	}

	/**
	 * Picks a rank from already-normalized odds using an inclusive 1..100 roll.
	 * Ranks configured to zero can never be drawn.
	 */
	public static int weightedRank(int[] normalizedOdds, int roll) {
		int bounded = Math.max(1, Math.min(100, roll));
		int cumulative = 0;
		for (int rank = 1; rank <= RANK_COUNT; rank++) {
			cumulative += normalizedOdds[rank - 1];
			if (bounded <= cumulative)
				return rank;
		}
		// Odds that fall short of 100 settle on the highest reachable rank
		// rather than silently promoting the roll to S.
		for (int rank = RANK_COUNT; rank >= 1; rank--) {
			if (normalizedOdds[rank - 1] > 0)
				return rank;
		}
		return 1;
	}

	/** An S-Rank reveal runs longer so the error report has room to play. */
	public static int rankRevealDuration(int rank) {
		return rank == RANK_COUNT
				? RANK_REVEAL_TICKS + S_RANK_ERROR_TICKS : RANK_REVEAL_TICKS;
	}

	/**
	 * Fraction of an S-Rank reveal spent showing the error before the result
	 * resolves. Shared so the client's presentation and the server's phase length
	 * cannot drift apart.
	 */
	public static float sRankErrorFraction() {
		return S_RANK_ERROR_TICKS
				/ (float) (RANK_REVEAL_TICKS + S_RANK_ERROR_TICKS);
	}

	/**
	 * Draws one class from a shuffle bag. A depleted bag is refilled with all six
	 * classes, while the current class is excluded from the first draw of the new
	 * bag so a reroll can never return the same result immediately.
	 */
	/**
	 * Draws from the built-in six.
	 *
	 * <p>Kept free of the class registry so the draw stays testable on its own.
	 * Callers that can see contributed classes pass their mask to the overload.
	 */
	public static ClassDraw drawClass(int remainingMask, int currentClass,
			int randomIndex) {
		return drawClass(remainingMask, currentClass, randomIndex, ALL_CLASSES_MASK);
	}

	/** @param drawable every class that may be drawn, contributed ones included */
	public static ClassDraw drawClass(int remainingMask, int currentClass,
			int randomIndex, int drawable) {
		int mask = remainingMask & drawable;
		int currentBit = classBit(currentClass);
		int candidates = mask & ~currentBit;
		if (candidates == 0) {
			mask = drawable;
			candidates = mask & ~currentBit;
		}
		if (candidates == 0)
			throw new IllegalStateException("Hunter Evaluation has no class candidates");

		int wanted = Math.floorMod(randomIndex, Integer.bitCount(candidates));
		int chosen = 0;
		for (int classId = 1; classId <= MAX_CLASS_ID; classId++) {
			int bit = classBit(classId);
			if ((candidates & bit) == 0)
				continue;
			if (wanted-- == 0) {
				chosen = classId;
				break;
			}
		}
		return new ClassDraw(chosen, mask & ~classBit(chosen));
	}

	public static int phaseDuration(Mode mode, Phase phase) {
		return switch (phase) {
			case BOOT -> mode == Mode.INITIAL ? INITIAL_BOOT_TICKS : 0;
			case CONTACT -> CONTACT_TICKS;
			case SCAN -> mode == Mode.INITIAL
					? INITIAL_SCAN_TICKS : REEVALUATION_SCAN_TICKS;
			case CLASS_REVEAL -> mode == Mode.INITIAL
					? INITIAL_CLASS_REVEAL_TICKS : 0;
			case RANK_REVEAL -> RANK_REVEAL_TICKS;
			case SETTLE -> mode == Mode.INITIAL ? INITIAL_SETTLE_TICKS : 0;
			case REROLL -> REROLL_TICKS;
			case STYLE_REVEAL -> STYLE_REVEAL_TICKS;
			case REROLL_STYLE -> REROLL_STYLE_TICKS;
			default -> 0;
		};
	}

	public static String className(int classId) {
		Optional<HunterClass> contributed = contributedClass(classId);
		if (contributed.isPresent())
			return contributed.get().displayName().getString();
		return CLASS_NAMES[clamp(classId, 0, CLASS_COUNT)];
	}

	/** One line about the class, or empty for a built-in, which shows none. */
	public static String classDescription(int classId) {
		return contributedClass(classId)
			.flatMap(value -> HunterClassRegistry.presentation(value.id()))
			.map(HunterClassPresentation::description)
			.orElse("");
	}

	public static int classColor(int classId) {
		Optional<HunterClassPresentation> contributed = contributedClass(classId)
			.flatMap(value -> HunterClassRegistry.presentation(value.id()));
		if (contributed.isPresent())
			return contributed.get().color();
		return CLASS_COLORS[clamp(classId, 0, CLASS_COUNT)];
	}

	public static String rankName(int rank) {
		return RANK_NAMES[clamp(rank, 0, 6)];
	}

	public static float rankIntensity(int rank) {
		return RANK_INTENSITIES[clamp(rank, 0, 6)];
	}

	public static boolean revealsClass(Mode mode, Phase phase) {
		if (mode == Mode.REEVALUATION)
			return true;
		return phase == Phase.CLASS_REVEAL || phase == Phase.RANK_REVEAL
				|| phase == Phase.SETTLE || phase == Phase.DECISION
				|| phase == Phase.REROLL || phase == Phase.COMPLETE
				|| phase == Phase.STYLE_REVEAL
				|| phase == Phase.REROLL_STYLE;
	}

	public static boolean revealsStyle(Mode mode, int classId, Phase phase) {
		if (!ClassStyleRules.supportsStyles(classId))
			return false;
		if (mode == Mode.REEVALUATION)
			return true;
		return phase == Phase.STYLE_REVEAL
				|| phase == Phase.RANK_REVEAL || phase == Phase.SETTLE
				|| phase == Phase.DECISION
				|| phase == Phase.REROLL_STYLE
				|| phase == Phase.COMPLETE;
	}

	public static boolean revealsRank(Phase phase) {
		return phase == Phase.RANK_REVEAL || phase == Phase.SETTLE
				|| phase == Phase.DECISION || phase == Phase.REROLL
				|| phase == Phase.COMPLETE;
	}

	private static int classBit(int classId) {
		return classId >= 1 && classId <= MAX_CLASS_ID
				? 1 << (classId - 1) : 0;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public enum Mode {
		INITIAL,
		REEVALUATION;

		public static Mode fromId(int id) {
			return id == REEVALUATION.ordinal() ? REEVALUATION : INITIAL;
		}
	}

	public enum Phase {
		BOOT,
		CONTACT,
		SCAN,
		CLASS_REVEAL,
		RANK_REVEAL,
		SETTLE,
		DECISION,
		REROLL,
		COMPLETE,
		/** Appended to preserve all pre-style save/wire ordinals. */
		STYLE_REVEAL,
		/** Appended to preserve all pre-style save/wire ordinals. */
		REROLL_STYLE;

		public static Phase fromId(int id) {
			Phase[] values = values();
			return id >= 0 && id < values.length ? values[id] : CONTACT;
		}
	}

	public enum Action {
		BEGIN_CONTACT,
		CANCEL_CONTACT,
		REROLL_CLASS,
		ACCEPT_RESULT,
		ACKNOWLEDGE,
		/** Appended to preserve all pre-style client/server action IDs. */
		REROLL_STYLE;

		public static Action fromId(int id) {
			Action[] values = values();
			return id >= 0 && id < values.length ? values[id] : null;
		}
	}

	public record ClassDraw(int classId, int remainingMask) {
	}
}
