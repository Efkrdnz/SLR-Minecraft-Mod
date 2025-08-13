package net.solocraft.procedures;

import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.world.level.LevelAccessor;

public class IsMiscEnabledProcedure {
	public static boolean execute(LevelAccessor world) {
		if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_MISC_ITEMS)) {
			return true;
		}
		return false;
	}
}
