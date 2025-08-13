package net.solocraft.procedures;

import net.solocraft.entity.StoneGolemEntity;

import net.minecraft.world.entity.Entity;

public class StoneGolemOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof StoneGolemEntity) {
			((StoneGolemEntity) entity).setAnimation("spawn");
		}
	}
}
