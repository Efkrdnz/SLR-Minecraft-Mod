package net.solocraft.procedures;

import net.solocraft.entity.BeruDeadBodyEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class BeruDeadBodyOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.getLevelData().getGameTime() % 20 == 0) {
			if (entity instanceof BeruDeadBodyEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruDeadBodyEntity.DATA_life, (int) ((entity instanceof BeruDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(BeruDeadBodyEntity.DATA_life) : 0) + 1));
			if ((entity instanceof BeruDeadBodyEntity _datEntI ? _datEntI.getEntityData().get(BeruDeadBodyEntity.DATA_life) : 0) >= 25) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
