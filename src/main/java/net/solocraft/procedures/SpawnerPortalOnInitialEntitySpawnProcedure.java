package net.solocraft.procedures;

import net.solocraft.entity.SpawnerPortalEntity;

import net.minecraft.world.entity.Entity;

public class SpawnerPortalOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof SpawnerPortalEntity) {
			((SpawnerPortalEntity) entity).setAnimation("spawn");
		}
		entity.getPersistentData().putDouble("portalspan", 0);
	}
}
