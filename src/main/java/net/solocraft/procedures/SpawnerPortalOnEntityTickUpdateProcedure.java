package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.SpawnerPortalEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class SpawnerPortalOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double rand2 = 0;
		rand = Math.random();
		if (rand >= 0.99) {
			rand2 = Mth.nextInt(RandomSource.create(), 1, 3);
			if (rand2 == 1) {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = SololevelingModEntities.D_KNIGHT_1.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
						entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
					}
				}
			} else if (rand2 == 2) {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = SololevelingModEntities.D_KNIGHT_2.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
						entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
					}
				}
			} else if (rand2 == 3) {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = SololevelingModEntities.D_KNIGHT_3.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
						entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
					}
				}
			}
		}
		if (entity instanceof SpawnerPortalEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SpawnerPortalEntity.DATA_life, (int) ((entity instanceof SpawnerPortalEntity _datEntI ? _datEntI.getEntityData().get(SpawnerPortalEntity.DATA_life) : 0) + 1));
		if ((entity instanceof SpawnerPortalEntity _datEntI ? _datEntI.getEntityData().get(SpawnerPortalEntity.DATA_life) : 0) >= 2000) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
