package net.solocraft.util;

/** Shared spendable-stat rewards for positive System level gains. */
public final class LevelRewardRules {
	public static final int SKILL_POINTS_PER_LEVEL = 3;

	private LevelRewardRules() {
	}

	public static int skillPointsForLevels(int levelsGained) {
		return Math.max(0, levelsGained) * SKILL_POINTS_PER_LEVEL;
	}
}
