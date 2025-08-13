package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.ShadowSoulEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class ShadowSoulOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 3, 0.01, 0.01, 0.01, 0);
		if (world.getLevelData().getGameTime() % 20 == 0) {
			if (entity instanceof ShadowSoulEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ShadowSoulEntity.DATA_life, (int) ((entity instanceof ShadowSoulEntity _datEntI ? _datEntI.getEntityData().get(ShadowSoulEntity.DATA_life) : 0) + 1));
			if ((entity instanceof ShadowSoulEntity _datEntI ? _datEntI.getEntityData().get(ShadowSoulEntity.DATA_life) : 0) >= 20) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ariset == 2) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
