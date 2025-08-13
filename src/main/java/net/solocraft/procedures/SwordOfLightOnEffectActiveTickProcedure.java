package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class SwordOfLightOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, double x, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_YELLOW.get()), x, (entity.getY() + entity.getBbHeight() / 2), z, 2, (entity.getBbWidth() / 1.5), (entity.getBbHeight() / 2), (entity.getBbWidth() / 1.5),
					0);
	}
}
