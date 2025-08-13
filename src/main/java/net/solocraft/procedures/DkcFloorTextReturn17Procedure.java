package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class DkcFloorTextReturn17Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		double floor = 0;
		floor = 17;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared > floor) {
			return "Cleared";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared == floor) {
			return "Current";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared == floor - 1) {
			return "Not Yet Unlocked!";
		}
		return "";
	}
}
