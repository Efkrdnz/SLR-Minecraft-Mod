package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.Entity;

public class SkillTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String selectedPower = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower;
		return ShadowMonarchManager.displaySkillName(entity, selectedPower);
	}
}
