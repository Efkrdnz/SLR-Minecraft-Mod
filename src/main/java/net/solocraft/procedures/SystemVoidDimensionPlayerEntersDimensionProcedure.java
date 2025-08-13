package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class SystemVoidDimensionPlayerEntersDimensionProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.setNoGravity(true);
	}
}
