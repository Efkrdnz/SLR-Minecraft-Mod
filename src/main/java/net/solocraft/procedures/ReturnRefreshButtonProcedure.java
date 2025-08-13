package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ReturnRefreshButtonProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "Refresh the store (" + Math.round((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).daily_refreshes) + " Refreshes remaining!)";
	}
}
