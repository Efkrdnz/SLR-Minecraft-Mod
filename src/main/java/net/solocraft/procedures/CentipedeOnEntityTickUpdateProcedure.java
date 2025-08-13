package net.solocraft.procedures;

import net.solocraft.entity.CentipedeEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class CentipedeOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.getLevelData().getGameTime() % 20 == 0) {
			if (entity instanceof CentipedeEntity _datEntSetI)
				_datEntSetI.getEntityData().set(CentipedeEntity.DATA_life, (int) ((entity instanceof CentipedeEntity _datEntI ? _datEntI.getEntityData().get(CentipedeEntity.DATA_life) : 0) + 1));
			if ((entity instanceof CentipedeEntity _datEntI ? _datEntI.getEntityData().get(CentipedeEntity.DATA_life) : 0) >= 100) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
