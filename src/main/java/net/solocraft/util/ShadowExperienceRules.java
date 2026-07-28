package net.solocraft.util;

/**
 * Pure balancing rules for shadow-soldier combat experience.
 *
 * <p>The target creates one XP pool from combat-relevant stats. Each shadow
 * receives only the part of that pool represented by its final, post-mitigation
 * damage. Low-level shadows receive a bounded catch-up multiplier which fades
 * out completely after level 30.</p>
 */
public final class ShadowExperienceRules {
	private static final int MAX_TARGET_XP_POOL = 100_000;

	private ShadowExperienceRules() {
	}

	public static int targetXpPool(double maxHealth, double attackDamage,
			double armor, double armorToughness, double targetLevel,
			int configuredBaseXp, boolean elite, boolean boss,
			boolean passiveAnimal) {
		double health = finiteNonNegative(maxHealth);
		double attack = finiteNonNegative(attackDamage);
		double protection = finiteNonNegative(armor);
		double toughness = finiteNonNegative(armorToughness);
		double level = finiteNonNegative(targetLevel);

		// Health measures the work required to win, while damage and defenses
		// represent the risk. Entity size deliberately has no influence.
		double naturalStrength = 2.0D
				+ health * 0.35D
				+ attack * 2.0D
				+ protection * 0.75D
				+ toughness * 1.5D
				+ level * 0.5D;

		if (passiveAnimal)
			naturalStrength *= 0.25D;
		if (boss)
			naturalStrength = naturalStrength * 2.5D + 40.0D;
		else if (elite)
			naturalStrength *= 1.5D;

		// Procedural/datapack encounters can supply their own player-XP value.
		// Treat ten percent as a floor, never as a reason to erase stat-based XP.
		double configuredFloor = configuredBaseXp > 0
				? configuredBaseXp * 0.10D : 0.0D;
		double minimum = passiveAnimal ? 1.0D : 5.0D;
		double result = Math.max(minimum,
				Math.max(naturalStrength, configuredFloor));
		if (!Double.isFinite(result))
			return MAX_TARGET_XP_POOL;
		return Math.max(1, (int) Math.min(MAX_TARGET_XP_POOL,
				Math.ceil(result)));
	}

	public static int contributionXp(int targetXpPool, double shadowDamage,
			double countedTargetDamage, int shadowLevel) {
		if (targetXpPool <= 0 || !Double.isFinite(shadowDamage)
				|| !Double.isFinite(countedTargetDamage)
				|| shadowDamage <= 0.0D || countedTargetDamage <= 0.0D)
			return 0;
		double share = Math.min(1.0D,
				Math.max(0.0D, shadowDamage / countedTargetDamage));
		double earned = targetXpPool * share
				* catchUpMultiplier(shadowLevel);
		if (!Double.isFinite(earned))
			return Integer.MAX_VALUE;
		return Math.max(1, (int) Math.min(Integer.MAX_VALUE,
				Math.round(earned)));
	}

	/**
	 * Fast through the first rank band, then progressively approaches the
	 * normal end-game rate. There is no catch-up multiplier above level 30.
	 */
	public static double catchUpMultiplier(int shadowLevel) {
		int level = Math.max(1, shadowLevel);
		if (level <= 10)
			return 2.5D;
		if (level <= 20)
			return 1.75D;
		if (level <= 30)
			return 1.25D;
		return 1.0D;
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
	}
}
