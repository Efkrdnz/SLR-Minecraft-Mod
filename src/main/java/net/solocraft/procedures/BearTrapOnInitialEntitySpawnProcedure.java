package net.solocraft.procedures;

import net.solocraft.entity.BearTrapEntity;

import net.minecraft.world.entity.Entity;

public class BearTrapOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BearTrapEntity) {
			((BearTrapEntity) entity).setAnimation("spawn");
		}
	}
}
