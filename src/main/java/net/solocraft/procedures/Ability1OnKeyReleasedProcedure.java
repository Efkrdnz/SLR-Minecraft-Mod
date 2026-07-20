package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.BeastMonarchManager;
import net.solocraft.util.FrostMonarchManager;
import net.solocraft.util.GoliathCombatManager;
import net.solocraft.util.LiuZhigangCombatManager;

public class Ability1OnKeyReleasedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (FrostMonarchManager.isDirectAbilityMode(entity))
			return;
		if (BeastMonarchManager.isFangStance(entity)) {
			BeastMonarchManager.releasePredatorsIntercept(entity, 0);
			return;
		}
		if (GoliathCombatManager.isCombatStance(entity)) {
			GoliathCombatManager.releasePursuit(entity, 0);
			return;
		}
		if (LiuZhigangCombatManager.isCombatStance(entity)) {
			LiuZhigangCombatManager.releaseDragonFlash(entity, 0);
			return;
		}
		if (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).combatmode)
			return;
		{
			double _setval = 0;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.firecharge = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		if (entity instanceof LivingEntity _entity)
			_entity.removeEffect(SololevelingModMobEffects.USING_FIRE.get());
		CooldownManager.clear(entity, "mana_refresh");
		{
			boolean _setval = false;
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.monarchbeam = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 2) {
			CooldownManager.set(entity, "job_1", 20);
			FireChargeInitialReleaseProcedure.execute(world, x, y, z, entity);
		}
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 4) {
			CooldownManager.set(entity, "job_1", 60);
		}
	}
}
