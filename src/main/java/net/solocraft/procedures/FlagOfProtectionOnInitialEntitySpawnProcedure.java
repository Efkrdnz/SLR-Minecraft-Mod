package net.solocraft.procedures;

import net.solocraft.entity.FlagOfProtectionEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

public class FlagOfProtectionOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof FlagOfProtectionEntity) {
			((FlagOfProtectionEntity) entity).setAnimation("spawnin");
		}
		SololevelingMod.queueServerWork(180, () -> {
			if (entity instanceof FlagOfProtectionEntity) {
				((FlagOfProtectionEntity) entity).setAnimation("despawn");
			}
			SololevelingMod.queueServerWork(20, () -> {
				if (!entity.level().isClientSide())
					entity.discard();
			});
		});
	}
}
