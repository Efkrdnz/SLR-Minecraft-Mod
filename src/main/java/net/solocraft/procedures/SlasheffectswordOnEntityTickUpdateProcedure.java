package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class SlasheffectswordOnEntityTickUpdateProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		entity.getPersistentData().putDouble("life", (entity.getPersistentData().getDouble("life") + 1));
		if (entity.getPersistentData().getDouble("life") >= 8) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
