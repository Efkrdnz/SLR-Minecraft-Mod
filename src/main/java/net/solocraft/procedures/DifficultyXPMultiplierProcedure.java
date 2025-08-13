package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.Difficulty;

public class DifficultyXPMultiplierProcedure {
	public static double execute(LevelAccessor world) {
		if (world.getDifficulty() == Difficulty.PEACEFUL || world.getDifficulty() == Difficulty.EASY) {
			return 1;
		} else if (world.getDifficulty() == Difficulty.NORMAL) {
			return 0.75;
		} else if (world.getDifficulty() == Difficulty.HARD) {
			return 0.5;
		}
		return 1;
	}
}
