package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class SlashProjectileHitsBlockProcedure {
	public static void execute(Entity immediatesourceentity) {
		if (immediatesourceentity == null)
			return;
		if (!immediatesourceentity.level().isClientSide())
			immediatesourceentity.discard();
	}
}
