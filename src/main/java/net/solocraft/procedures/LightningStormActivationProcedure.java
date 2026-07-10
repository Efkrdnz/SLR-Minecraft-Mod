package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.solocraft.util.CooldownManager;

public class LightningStormActivationProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		CooldownManager.set(entity, "job_2", 400);
		{
			double _setval = 10;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.baranlightningstrike = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
	}
}
