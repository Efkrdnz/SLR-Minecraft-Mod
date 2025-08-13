package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ReturnRemainingXPProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "Progression: " + new java.text.DecimalFormat("##.#").format((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Xp) + " / "
				+ new java.text.DecimalFormat("##").format((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MaxXP);
	}
}
