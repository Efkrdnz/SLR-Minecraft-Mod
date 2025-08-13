package net.solocraft.procedures;

import net.solocraft.entity.IceChunkEntity;

import net.minecraft.world.entity.Entity;

public class IceChunkOnEntityTickUpdateProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof IceChunkEntity _datEntSetI)
			_datEntSetI.getEntityData().set(IceChunkEntity.DATA_life, (int) ((entity instanceof IceChunkEntity _datEntI ? _datEntI.getEntityData().get(IceChunkEntity.DATA_life) : 0) + 1));
		if ((entity instanceof IceChunkEntity _datEntI ? _datEntI.getEntityData().get(IceChunkEntity.DATA_life) : 0) == 70) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
