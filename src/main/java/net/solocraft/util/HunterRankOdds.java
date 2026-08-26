package net.solocraft.util;

import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bridges the per-world awakening odds game rules to the pure rank maths in
 * {@link HunterEvaluationRules}.
 *
 * <p>The rules hold raw weights rather than literal percentages. Nothing forces
 * them to total 100, so every read normalizes proportionally: weights of
 * 25/25/25/12/12/11 total 110 and yield an S chance of 10%.
 */
public final class HunterRankOdds {
	private static final List<GameRules.Key<GameRules.IntegerValue>> KEYS = List.of(
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_E,
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_D,
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_C,
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_B,
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_A,
			SololevelingModGameRules.SOLO_LEVELING_RANK_ODDS_S);

	private HunterRankOdds() {
	}

	/** Raw configured weights, in E..S order, exactly as the player set them. */
	public static int[] readWeights(GameRules rules) {
		int[] weights = new int[HunterEvaluationRules.RANK_COUNT];
		if (rules == null)
			return HunterEvaluationRules.DEFAULT_RANK_ODDS.clone();
		for (int index = 0; index < weights.length; index++)
			weights[index] = Math.max(0, Math.min(
					HunterEvaluationRules.MAX_RANK_WEIGHT,
					rules.getInt(KEYS.get(index))));
		return weights;
	}

	public static void writeWeights(GameRules rules, int[] weights) {
		if (rules == null || weights == null)
			return;
		for (int index = 0; index < KEYS.size() && index < weights.length; index++) {
			int weight = Math.max(0, Math.min(
					HunterEvaluationRules.MAX_RANK_WEIGHT, weights[index]));
			rules.getRule(KEYS.get(index)).set(weight, null);
		}
	}

	/** Configured weights rescaled to percentages totalling exactly 100. */
	public static int[] normalized(GameRules rules) {
		return HunterEvaluationRules.normalizedRankOdds(readWeights(rules));
	}

	public static int[] normalized(Level level) {
		return normalized(level == null ? null : level.getGameRules());
	}

	/** Rolls a rank for this world's configured odds. */
	public static int roll(Level level, int inclusiveRoll) {
		return HunterEvaluationRules.weightedRank(normalized(level), inclusiveRoll);
	}
}
