package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class DkcButtonVisibilityConditionProcedure {
	public static boolean execute(Entity entity, double floor) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (floor == 1) {
			return vars.dkc_started || vars.dkc_cleared > 0;
		}
		if (vars.dkc_cleared >= floor - 1) {
			return true;
		}
		return false;
	}
}
