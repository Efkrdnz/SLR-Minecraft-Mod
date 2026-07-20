package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.CombatRankHelper;

public class KasakasVenomFangsLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(world, entity, sourceentity, 3);
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity, int maximumTargetRank) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get())) {
			if (world.isClientSide())
				return;
			boolean paralysisApplied = CombatRankHelper.isAtMost(entity, maximumTargetRank);
			if (paralysisApplied && entity instanceof LivingEntity target) {
				target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 10));
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 10));
				target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 10));
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 10));
			}
			CooldownManager.set(sourceentity, "paralyze", 200);
			if (sourceentity instanceof LivingEntity _entity)
				_entity.removeEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
			if (paralysisApplied) {
				boolean notificationVisible = true;
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.paralyzenot = notificationVisible;
					capability.syncPlayerVariables(sourceentity);
				});
				SololevelingMod.queueServerWork(40, () -> {
					boolean notificationHidden = false;
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.paralyzenot = notificationHidden;
						capability.syncPlayerVariables(sourceentity);
					});
				});
			}
		}
	}
}
