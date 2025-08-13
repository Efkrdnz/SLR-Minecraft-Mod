package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ReturnPotionf1Procedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).golds >= 100) {
			return "\u00A7l" + "Restores 10 FTG " + "\u00A7b" + "100 Gold";
		}
		return "\u00A7l" + "Restores 10 FTG " + "\u00A7c" + "100 Gold";
	}
}
