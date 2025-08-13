package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class UseSkillOnKeyReleasedProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(SololevelingModMobEffects.CONSECUTIVE_SLASHES.get());
		{
			double _setval = 0;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.firecharge = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Critical Attack")) {
			if (entity.getPersistentData().getBoolean("Critical_Attack_Targetting")) {
				CriticalAttackUseProcedure.execute(world, entity);
				entity.getPersistentData().putBoolean("Critical_Attack_Targetting", false);
				entity.getPersistentData().putString("CriticalAttackTarget", "");
			}
		}
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).PselectedPower).equals("Mutilation")) {
			if (entity.getPersistentData().getBoolean("Mutilation_Targetting")) {
				MutilationUseProcedure.execute(world, entity);
				entity.getPersistentData().putBoolean("Mutilation_Targetting", false);
				entity.getPersistentData().putString("MutilationTarget", "");
			}
		}
		entity.getPersistentData().putBoolean("Critical_Attack_Targetting", false);
		entity.getPersistentData().putBoolean("Mutilation_Targetting", false);
		entity.getPersistentData().putString("CriticalAttackTarget", "");
	}
}
