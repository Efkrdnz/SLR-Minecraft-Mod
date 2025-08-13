package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.particles.ParticleTypes;

public class FireBallProjectileWhileProjectileFlyingTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		world.addParticle(ParticleTypes.LAVA, x, y, z, 0.1, 0.1, 0.1);
		world.addParticle(ParticleTypes.FLAME, x, y, z, 0.1, 0.1, 0.1);
	}
}
