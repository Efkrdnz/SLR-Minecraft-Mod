package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

public class IceBallRightClickedOnEntityProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double motionZ = 0;
		double deltaZ = 0;
		double deltaX = 0;
		double motionY = 0;
		double deltaY = 0;
		double motionX = 0;
		double speed = 0;
		if ((entity.getPersistentData().getString("caster")).equals(sourceentity.getDisplayName().getString())) {
			entity.getPersistentData().putString("state", "move");
			if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.JOB_COOLDOWN_1.get(), 100, 1, false, false));
			deltaX = -Math.sin((sourceentity.getYRot() / 180) * (float) Math.PI);
			deltaY = -Math.sin((sourceentity.getXRot() / 180) * (float) Math.PI);
			deltaZ = Math.cos((sourceentity.getYRot() / 180) * (float) Math.PI);
			speed = 2;
			motionX = deltaX * speed;
			motionY = deltaY * speed;
			motionZ = deltaZ * speed;
			entity.setDeltaMovement(entity.getDeltaMovement().add(motionX, motionY, motionZ));
			entity.getPersistentData().putDouble("IceX", (-Math.sin((sourceentity.getYRot() / 180) * (float) Math.PI)));
			entity.getPersistentData().putDouble("IceY", (-Math.sin((sourceentity.getXRot() / 180) * (float) Math.PI)));
			entity.getPersistentData().putDouble("IceZ", Math.cos((sourceentity.getYRot() / 180) * (float) Math.PI));
			sourceentity.getPersistentData().putBoolean("UsingIceBall", false);
		}
	}
}
