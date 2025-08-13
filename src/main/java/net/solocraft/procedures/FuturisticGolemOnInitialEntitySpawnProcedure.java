package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class FuturisticGolemOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putString("state", "idle");
	}
}
