package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.particles.SimpleParticleType;

public class BloodParticleAdditionalParticleExpiryConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z, boolean onGround) {
		if (onGround == true) {
			world.addParticle((SimpleParticleType) (SololevelingModParticleTypes.BLOOD_PARTICLE_LAND.get()), x, y, z, 0, 0, 0);
			return true;
		}
		return false;
	}
}
