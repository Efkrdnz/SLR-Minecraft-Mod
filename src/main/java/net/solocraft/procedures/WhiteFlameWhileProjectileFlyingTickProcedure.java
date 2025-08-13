package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

public class WhiteFlameWhileProjectileFlyingTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
		if (immediatesourceentity == null)
			return;
		if (Math.random() < 0.5) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.WHITE_FLAMES.get()), x, y, z, 1, 0.02, 0.02, 0.02, 0.1);
		}
		if (Math.random() < 0.2) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.02, 0.02, 0.02, 0);
		}
		Vec3 motion = immediatesourceentity.getDeltaMovement().scale(1);
		immediatesourceentity.setDeltaMovement(motion);
		immediatesourceentity.setNoGravity(true);
		if (immediatesourceentity.getPersistentData().getDouble("Timer") < 10) {
			immediatesourceentity.getPersistentData().putDouble("Timer", (immediatesourceentity.getPersistentData().getDouble("Timer") + 1));
		} else {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
