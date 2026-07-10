package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class DkcFloorTextReturnProcedure {
	public static String execute(Entity entity, double floor) {
		if (entity == null)
			return "";
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (floor == 1 && !vars.dkc_started && vars.dkc_cleared <= 0) {
			return "Sealed";
		}
		if (vars.dkc_cleared >= floor) {
			return "Cleared";
		} else if (vars.dkc_cleared == floor - 1) {
			return "Current";
		} else if (vars.dkc_cleared < floor - 1) {
			return "Not Yet Unlocked!";
		}
		return "";
	}
}
