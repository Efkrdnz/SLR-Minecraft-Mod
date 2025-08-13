package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class IceBallOnInitialEntitySpawnProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putString("state", "");
		entity.getPersistentData().putDouble("IceLife", 0);
	}
}
