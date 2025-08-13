package net.solocraft.procedures;

import net.solocraft.entity.ArrowSplashEntity;

import net.minecraft.world.entity.Entity;

public class ArrowSplashOnEntityTickUpdateProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ArrowSplashEntity _datEntSetI)
			_datEntSetI.getEntityData().set(ArrowSplashEntity.DATA_life, (int) ((entity instanceof ArrowSplashEntity _datEntI ? _datEntI.getEntityData().get(ArrowSplashEntity.DATA_life) : 0) + 1));
		if ((entity instanceof ArrowSplashEntity _datEntI ? _datEntI.getEntityData().get(ArrowSplashEntity.DATA_life) : 0) >= 20) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
