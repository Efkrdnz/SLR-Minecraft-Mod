package net.solocraft.procedures;

import net.solocraft.entity.ShadowSoulEntity;
import net.solocraft.util.AriseExtractionRules;

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
		int failures = entity.getPersistentData().contains(
				AriseExtractionRules.FAILURE_COUNT_TAG)
						? entity.getPersistentData().getInt(
								AriseExtractionRules.FAILURE_COUNT_TAG)
						: (int) Math.floor(
								entity.getPersistentData().getDouble("ariset"));
		if (AriseExtractionRules.failuresExhausted(failures)) {
			if (!entity.level().isClientSide())
				entity.discard();
			return;
		}
		if (AriseExtractionRules.isBossSoul(
				entity.getPersistentData().getString("soultype")))
			return;
		if (world.getLevelData().getGameTime() % 20 == 0) {
			if (entity instanceof ShadowSoulEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ShadowSoulEntity.DATA_life, (int) ((entity instanceof ShadowSoulEntity _datEntI ? _datEntI.getEntityData().get(ShadowSoulEntity.DATA_life) : 0) + 1));
			if ((entity instanceof ShadowSoulEntity _datEntI ? _datEntI.getEntityData().get(ShadowSoulEntity.DATA_life) : 0) >= 20) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
