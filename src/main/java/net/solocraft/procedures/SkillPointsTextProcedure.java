package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SkillPointRules;

import net.minecraft.world.entity.Entity;

public class SkillPointsTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "" + SkillPointRules.spendable((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).SkillPoints);
	}
}
