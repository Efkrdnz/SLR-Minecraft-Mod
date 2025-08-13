package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class DaggerSlashOnEntityTickUpdateProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("Life", (entity.getPersistentData().getDouble("Life") + 1));
		if (entity.getPersistentData().getDouble("Life") >= 15) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
