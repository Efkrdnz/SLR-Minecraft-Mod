package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class RunTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "[" + new java.text.DecimalFormat("##.##").format((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).RUN / 50) + "/"
				+ new java.text.DecimalFormat("##.##").format(DailyQuestHelper.visibleRunTarget(entity) / 50) + "KM]";
	}
}
