package net.solocraft.util;

/** Dependency-free balance rules for restoring summoned shadow health. */
public final class ShadowHealingRules {
	public static final double HEALTH_PER_MANA = 4.0D;

	private ShadowHealingRules() {
	}

	public static int manaCost(double missingHealth) {
		if (!Double.isFinite(missingHealth) || missingHealth <= 0.0D)
			return 0;
		double cost = Math.ceil(missingHealth / HEALTH_PER_MANA);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, cost));
	}
}
