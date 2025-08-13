package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class DragonBreatheWhileProjectileFlyingTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity) {
		if (entity == null || immediatesourceentity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.FLAME, x, y, z, 20, 1, 1, 1, 0);
		if (Math.sqrt(Math.pow(entity.getX() - immediatesourceentity.getX(), 2) + Math.pow(entity.getY() - immediatesourceentity.getY(), 2) + Math.pow(entity.getZ() - immediatesourceentity.getZ(), 2)) >= 20) {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
