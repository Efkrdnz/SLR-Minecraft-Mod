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

public class KasakasVenomFangsLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get())) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 10));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 10));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 10));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 10));
			CooldownManager.set(sourceentity, "paralyze", 200);
			if (sourceentity instanceof LivingEntity _entity)
				_entity.removeEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
			{
				boolean _setval = true;
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.paralyzenot = _setval;
					capability.syncPlayerVariables(sourceentity);
				});
			}
			SololevelingMod.queueServerWork(40, () -> {
				{
					boolean _setval = false;
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.paralyzenot = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
			});
		}
	}
}
