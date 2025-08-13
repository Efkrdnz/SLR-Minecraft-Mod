package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ReturnTitleProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).title == 1) {
			return "\u00A7f\u00A7l" + "Title: " + "Wolf Assassin";
		}
		return "\u00A7f\u00A7l" + "Title: " + "None";
	}
}
