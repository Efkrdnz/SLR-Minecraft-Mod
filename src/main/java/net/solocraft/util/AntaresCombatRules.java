package net.solocraft.util;

/** Pure balance rules shared by Antares combat code and regression tests. */
final class AntaresCombatRules {
	static final int MAX_RUIN = 3;
	static final int BREATH_LEVEL = 55;
	static final int DESCENT_LEVEL = 70;
	static final int ROAR_LEVEL = 85;
	static final int EXTINCTION_LEVEL = 100;
	static final int MANIFESTATION_LEVEL = 120;

	private AntaresCombatRules() {
	}

	static int clampRuin(int charges) {
		return Math.max(0, Math.min(MAX_RUIN, charges));
	}

	static int gainRuin(int current, int amount) {
		return clampRuin(current + Math.max(0, amount));
	}

	static boolean canSpendFullRuin(int charges) {
		return clampRuin(charges) == MAX_RUIN;
	}

	static float playerDamage(float damage) {
		return Math.max(0.5F, damage * 0.68F);
	}

	static double bossControlScale(boolean boss) {
		return boss ? 0.28D : 1.0D;
	}
}
