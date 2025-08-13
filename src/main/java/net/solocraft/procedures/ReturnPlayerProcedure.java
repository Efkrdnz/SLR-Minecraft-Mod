package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class ReturnPlayerProcedure {
	public static Entity execute(Entity entity) {
		if (entity == null)
			return null;
		return entity;
	}
}
