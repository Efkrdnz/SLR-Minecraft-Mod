package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

public class NoFallDamageOnEffectActiveTickProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.fallDistance = 0;
		if (entity.onGround()) {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(SololevelingModMobEffects.NO_FALL_DAMAGE.get());
		}
	}
}
