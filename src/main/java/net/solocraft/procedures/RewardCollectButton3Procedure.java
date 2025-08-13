package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class RewardCollectButton3Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		String reward_to_collect = "";
		reward_to_collect = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_3;
		RewardCollectProcedure.execute(entity, reward_to_collect);
		{
			String _setval = "";
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.reward_3 = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
	}
}
