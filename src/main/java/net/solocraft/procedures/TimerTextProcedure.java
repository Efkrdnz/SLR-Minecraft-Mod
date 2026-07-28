package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class TimerTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		double timer = entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dailytimer;
		if (timer > 6000)
			return "\u00A7l\u00A7fTime: \u00A7f" + Math.round(timer / 20);
		if (timer > 1200)
			return "\u00A7l\u00A7fTime: \u00A76" + Math.round(timer / 20);
		if (timer > 0)
			return "\u00A7l\u00A7fTime: \u00A74" + Math.round(timer / 20);
		return "\u00A7fNo Active Mission";
	}
}
