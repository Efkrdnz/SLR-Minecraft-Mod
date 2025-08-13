package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class GoldTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		return "\u00A76" + "Total Gold:" + " " + Math.round((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).golds);
	}
}
