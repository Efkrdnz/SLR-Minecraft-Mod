package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class DoesHaveKaiselProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		return (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Kaisel > 0;
	}
}
