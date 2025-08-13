package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

public class ManaBulletWhileProjectileFlyingTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
		if (immediatesourceentity == null)
			return;
		immediatesourceentity.setNoGravity(true);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.END_ROD, x, y, z, 10, 0.2, 0.2, 0.2, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_RED.get()), x, y, z, 5, 0.05, 0.05, 0.05, 0);
		immediatesourceentity.getPersistentData().putDouble("TimerT", (immediatesourceentity.getPersistentData().getDouble("TimerT") + 1));
		if (immediatesourceentity.getPersistentData().getDouble("TimerT") >= 10) {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
