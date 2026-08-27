package net.solocraft.procedures;

import net.solocraft.entity.BeruDeadBodyEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class BeruDeadBodyOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BeruDeadBodyEntity beru
				&& beru.getEntityData().get(BeruDeadBodyEntity.DATA_tries) <= 0
				&& !entity.level().isClientSide())
			entity.discard();
	}
}
