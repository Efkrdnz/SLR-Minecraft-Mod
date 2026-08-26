package net.solocraft.util;

import java.util.Locale;

/**
 * What the player becomes after releasing the System at the Statue of God.
 *
 * <p>The Shadow Monarch inherits the Black Heart; every other vessel gets a True
 * Vessel Awakening. Both are terminal states granted once, at the end of the
 * Cartenon return, and both are stored in one persisted string so the set can
 * grow later without touching the variable schema again.
 *
 * <p>Dependency-free on purpose. The Black Heart's mana grant is the single most
 * dangerous number in the mod -- a hundred thousand mana sitting next to a cost
 * system that works in fractions of the pool -- so it needs to be assertable in
 * a test that does not require Minecraft on the classpath.
 */
public final class TrueMonarchRules {
	public static final String NONE = "";
	/** Shadow Monarch only. */
	public static final String BLACK_HEART = "black";
	/** Every other vessel. */
	public static final String TRUE_VESSEL = "vessel";

	/**
	 * Flat reservoir granted by the Black Heart.
	 *
	 * <p>This is added to maximum mana <b>without</b> touching Intelligence, and
	 * every mana cost in the mod derives from {@link ManaRules#costBasis} rather
	 * than from the reservoir precisely so this number cannot inflate what
	 * anything costs. Wiring a cost to the reservoir instead would multiply that
	 * cost by roughly twenty-five.
	 */
	public static final double BLACK_HEART_MANA = 100_000.0D;

	/** Sentinel for a shadow storage capacity with no ceiling. */
	public static final int UNLIMITED_STORAGE = Integer.MAX_VALUE;

	private TrueMonarchRules() {
	}

	/** Normalizes any stored or supplied value to one of the three states. */
	public static String normalize(String heart) {
		if (heart == null)
			return NONE;
		String cleaned = heart.trim().toLowerCase(Locale.ROOT);
		return switch (cleaned) {
			case BLACK_HEART -> BLACK_HEART;
			case TRUE_VESSEL -> TRUE_VESSEL;
			default -> NONE;
		};
	}

	/** The reward a vessel earns. Job id 1 is the Shadow Monarch. */
	public static String rewardForJob(int jobId) {
		return jobId == 1 ? BLACK_HEART : TRUE_VESSEL;
	}

	public static boolean isAwakened(String heart) {
		return !NONE.equals(normalize(heart));
	}

	public static boolean hasBlackHeart(String heart) {
		return BLACK_HEART.equals(normalize(heart));
	}

	/** Flat maximum-mana bonus. Never a cost basis. */
	public static double flatManaBonus(String heart) {
		return hasBlackHeart(heart) ? BLACK_HEART_MANA : 0.0D;
	}

	/** The Black Heart lifts the shadow storage ceiling entirely. */
	public static boolean hasUnlimitedShadowStorage(String heart) {
		return hasBlackHeart(heart);
	}

	/**
	 * Free shadow storage, given the tiered capacity and current usage.
	 * Returns {@link #UNLIMITED_STORAGE} rather than a merely huge number so
	 * callers cannot accidentally do arithmetic that overflows.
	 */
	public static int freeShadowStorage(String heart, double capacity, double usage) {
		if (hasUnlimitedShadowStorage(heart))
			return UNLIMITED_STORAGE;
		return (int) Math.max(0, Math.floor(capacity - usage));
	}

	public static String displayName(String heart) {
		return switch (normalize(heart)) {
			case BLACK_HEART -> "Black Heart";
			case TRUE_VESSEL -> "True Vessel";
			default -> "";
		};
	}
}
