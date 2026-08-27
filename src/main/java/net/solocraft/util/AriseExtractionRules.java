package net.solocraft.util;

import java.util.Locale;

/**
 * Dependency-free balance rules for Shadow Monarch extraction attempts.
 */
public final class AriseExtractionRules {
	public static final String FAILURE_COUNT_TAG = "slr_arise_failures";
	public static final String TARGET_LEVEL_TAG = "slr_arise_target_level";
	public static final String EXTRACTION_OWNER_TAG = "slr_arise_owner";
	public static final int MAX_BOSS_EXTRACTION_FAILURES = 3;
	public static final double MAX_EXTRACTION_LEVEL_GAP = 75.0D;

	private AriseExtractionRules() {
	}

	public static boolean isBossSoul(String soulType) {
		return switch (normalize(soulType)) {
			case "igris", "beru", "tusk", "kaisel" -> true;
			default -> false;
		};
	}

	public static double defaultTargetLevel(String soulType) {
		return switch (normalize(soulType)) {
			case "orc", "bear" -> 40.0D;
			case "highorc" -> 50.0D;
			case "igris" -> 60.0D;
			case "tusk", "kaisel" -> 70.0D;
			case "beru" -> 100.0D;
			default -> 0.0D;
		};
	}

	public static double effectiveTargetLevel(String soulType,
			double storedTargetLevel) {
		return finiteNonNegative(storedTargetLevel) > 0.0D
				? finiteNonNegative(storedTargetLevel)
				: defaultTargetLevel(soulType);
	}

	public static boolean isOverwhelming(double playerLevel,
			double targetLevel, boolean creativeMode) {
		if (creativeMode)
			return false;
		return finiteNonNegative(targetLevel)
				> finiteNonNegative(playerLevel) + MAX_EXTRACTION_LEVEL_GAP;
	}

	public static double successChance(double playerLevel,
			double targetLevel, boolean creativeMode) {
		if (creativeMode)
			return 1.0D;
		double target = finiteNonNegative(targetLevel);
		if (target <= 0.0D)
			return 1.0D;
		if (isOverwhelming(playerLevel, target, false))
			return 0.0D;
		double levelGap = Math.max(0.0D,
				finiteNonNegative(targetLevel) - finiteNonNegative(playerLevel));
		// A player who personally earns a corpse should have a realistic first
		// and second attempt, while much stronger targets still resist often.
		return clamp(0.82D - levelGap * 0.008D, 0.25D, 0.96D);
	}

	/**
	 * Mana for a whole Arise, as a share of the caster's pool.
	 *
	 * <p>This deliberately is not a flat per-soul price. Arise used to cost a
	 * fixed 500 per shadow against a pool of {@code 1000 + 100 x Intelligence},
	 * and the Shadow Monarch is a Strength/Agility class that has no reason to
	 * buy Intelligence. A monarch sitting near the 1000 floor could afford two
	 * souls at completely full mana and exactly one at anything below it, so a
	 * cast over a pile of corpses raised a single shadow and said nothing about
	 * why -- the batch loop was never the problem.
	 *
	 * <p>Scaling off the pool keeps the cost meaningful at every Intelligence,
	 * and charging additional souls less than the first makes a mass raise the
	 * efficient way to spend the bar, which is the fantasy the skill is for.
	 */
	public static final double FIRST_SOUL_MANA_FRACTION = 0.15D;
	public static final double EXTRA_SOUL_MANA_FRACTION = 0.07D;

	public static double manaCostForSouls(double maximumMana, int souls) {
		int count = Math.max(0, souls);
		if (count <= 0)
			return 0.0D;
		return finiteNonNegative(maximumMana) * (FIRST_SOUL_MANA_FRACTION
				+ (count - 1) * EXTRA_SOUL_MANA_FRACTION);
	}

	/** The largest batch, up to {@code desired}, the caster can pay for. */
	public static int affordableSouls(double currentMana, double maximumMana,
			int desired) {
		double mana = finiteNonNegative(currentMana);
		for (int count = Math.max(0, desired); count > 0; count--) {
			if (manaCostForSouls(maximumMana, count) <= mana)
				return count;
		}
		return 0;
	}

	/** The third eligible extraction attempt is guaranteed to succeed. */
	public static boolean isGuaranteedAttempt(int previousFailures) {
		return Math.max(0, previousFailures)
				>= MAX_BOSS_EXTRACTION_FAILURES - 1;
	}

	public static int nextFailureCount(int currentFailures) {
		return Math.min(MAX_BOSS_EXTRACTION_FAILURES,
				Math.max(0, currentFailures) + 1);
	}

	public static boolean failuresExhausted(int failures) {
		return failures >= MAX_BOSS_EXTRACTION_FAILURES;
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
	}

	private static double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static String normalize(String soulType) {
		return soulType == null ? ""
				: soulType.trim().toLowerCase(Locale.ROOT)
						.replace("_", "").replace("-", "").replace(" ", "");
	}
}
