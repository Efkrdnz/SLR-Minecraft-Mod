package net.solocraft.util;

/**
 * Dependency-free rules for the end of the progression curve.
 *
 * <p>Levelling used to be unbounded: {@code LevelUpProcedure} looped until
 * {@code Integer.MAX_VALUE}, so the mod had no peak and no ending. The cap gives
 * the climb a summit, and reaching it is what opens the return to the Cartenon
 * Temple.
 *
 * <p>The XP curve is {@code requiredXp(level) = level * 16 + 8}, so the total to
 * reach level N is {@code sum(16L + 8) for L in [0, N)}, which collapses to
 * {@code 8N^2}. Both forms are exposed: the per-level cost because that is what
 * the level-up loop consumes, and the closed form because the UI and the tests
 * need totals without walking the curve.
 */
public final class LevelCapRules {
	/** The shipped cap. Sits past level 120, the last content unlock. */
	public static final int DEFAULT_LEVEL_CAP = 150;

	/** Highest cap the world-creation slider offers. */
	public static final int MAXIMUM_LEVEL_CAP = 300;

	/**
	 * A cap of zero means the old unbounded behaviour. Servers that do not want
	 * a finite progression need a way back, and rejecting the setting outright
	 * would make the gamerule a one-way door for existing worlds.
	 */
	public static final int UNLIMITED = 0;

	private LevelCapRules() {
	}

	/**
	 * Normalizes a raw gamerule value. Negative values are treated as unlimited
	 * rather than clamped to a level, because a negative cap most likely means a
	 * command typo and silently pinning the player to level 1 would be worse than
	 * leaving progression open.
	 */
	public static int resolveCap(int ruleValue) {
		if (ruleValue <= UNLIMITED)
			return UNLIMITED;
		return Math.min(MAXIMUM_LEVEL_CAP, ruleValue);
	}

	public static boolean isUnlimited(int cap) {
		return resolveCap(cap) == UNLIMITED;
	}

	/** True once the player has nothing left to climb. */
	public static boolean isCapped(int level, int cap) {
		int resolved = resolveCap(cap);
		return resolved != UNLIMITED && level >= resolved;
	}

	/** The highest level this cap allows, or the argument when uncapped. */
	public static int clampLevel(int level, int cap) {
		int resolved = resolveCap(cap);
		return resolved == UNLIMITED ? level : Math.min(level, resolved);
	}

	/** XP required to advance out of {@code level}. */
	public static double requiredXpFor(int level) {
		return Math.max(0, level) * 16.0D + 8.0D;
	}

	/** Total XP earned across a whole run to reach {@code level}: {@code 8N^2}. */
	public static double cumulativeXpFor(int level) {
		double n = Math.max(0, level);
		return 8.0D * n * n;
	}

	/** Remaining XP from {@code level} to the cap; zero when uncapped or done. */
	public static double xpRemainingTo(int level, int cap) {
		int resolved = resolveCap(cap);
		if (resolved == UNLIMITED || level >= resolved)
			return 0.0D;
		return cumulativeXpFor(resolved) - cumulativeXpFor(level);
	}
}
