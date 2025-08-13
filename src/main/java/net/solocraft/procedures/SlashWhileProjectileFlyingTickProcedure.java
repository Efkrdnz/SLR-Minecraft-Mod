package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class SlashWhileProjectileFlyingTickProcedure {
	public static void execute(Entity entity, Entity immediatesourceentity) {
		if (entity == null || immediatesourceentity == null)
			return;
		immediatesourceentity.setNoGravity(true);
		if (Math.sqrt(Math.pow(immediatesourceentity.getX() - entity.getX(), 2) + Math.pow(immediatesourceentity.getY() - entity.getY(), 2) + Math.pow(immediatesourceentity.getZ() - entity.getZ(), 2)) >= 16) {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
