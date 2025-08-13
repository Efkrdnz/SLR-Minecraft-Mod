package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class DkcButtonVisibilityCondition14Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		double floor = 0;
		floor = 14;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared >= floor - 1) {
			return true;
		}
		return false;
	}
}
