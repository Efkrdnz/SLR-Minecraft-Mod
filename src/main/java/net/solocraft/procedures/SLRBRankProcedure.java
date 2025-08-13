package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class SLRBRankProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		{
			String _setval = "B";
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.ranking = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
	}
}
