package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MageSpellProgression;

import net.minecraft.world.entity.Entity;

public class ReturnClassNameProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 1) {
			return "\u00A7bAssassin";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 2) {
			String specialization = MageSpellProgression.specialization(entity);
			return "barrier".equals(specialization) ? "\u00A7bBarrier Mage"
					: "arcane".equals(specialization) ? "\u00A7dArcane Mage"
					: "storm".equals(specialization) ? "\u00A7eStorm Mage"
					: "curse".equals(specialization) ? "\u00A75Curse Mage" : "\u00A7cFire Mage";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 3) {
			return "\u00A7cFighter";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 4) {
			return "\u00A7fTanker";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 5) {
			return "\u00A7aSupport Mage";
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Classes == 6) {
			return "\u00A72Ranger";
		}
		return " none";
	}
}
