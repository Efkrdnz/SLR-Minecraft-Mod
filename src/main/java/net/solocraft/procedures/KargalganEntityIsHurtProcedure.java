package net.solocraft.procedures;

import net.solocraft.entity.KargalganEntity;

import net.minecraft.world.entity.Entity;

public class KargalganEntityIsHurtProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof KargalganEntity _datEntSetI)
			_datEntSetI.getEntityData().set(KargalganEntity.DATA_Push, (int) ((entity instanceof KargalganEntity _datEntI ? _datEntI.getEntityData().get(KargalganEntity.DATA_Push) : 0) + 1));
	}
}
