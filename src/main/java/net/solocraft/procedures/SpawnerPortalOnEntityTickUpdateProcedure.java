package net.solocraft.procedures;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.SpawnerPortalEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;

public class SpawnerPortalOnEntityTickUpdateProcedure {
	private static final int LOCAL_KNIGHT_CAP = 10;
	private static final int DUNGEON_KNIGHT_CAP = 48;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof SpawnerPortalEntity portal))
			return;

		// Portals are owned by quest progress now; they no longer expire after 100 seconds.
		if (!JobChangeQuestManager.hasAdvancementPlayerNear(portal, 192.0D) || level.random.nextFloat() < 0.99F)
			return;

		AABB localArea = portal.getBoundingBox().inflate(18.0D, 8.0D, 18.0D);
		int nearbyKnights = level.getEntitiesOfClass(Entity.class, localArea,
				target -> target instanceof DKnight1Entity || target instanceof DKnight2Entity || target instanceof DKnight3Entity).size();
		if (nearbyKnights >= LOCAL_KNIGHT_CAP)
			return;
		AABB dungeonArea = portal.getBoundingBox().inflate(160.0D);
		int dungeonKnights = level.getEntitiesOfClass(Entity.class, dungeonArea,
				target -> target instanceof DKnight1Entity || target instanceof DKnight2Entity || target instanceof DKnight3Entity).size();
		if (dungeonKnights >= DUNGEON_KNIGHT_CAP)
			return;

		Entity spawned = switch (level.random.nextInt(3)) {
			case 0 -> SololevelingModEntities.D_KNIGHT_1.get().spawn(level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			case 1 -> SololevelingModEntities.D_KNIGHT_2.get().spawn(level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			default -> SololevelingModEntities.D_KNIGHT_3.get().spawn(level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
		};
		if (spawned != null) {
			spawned.setYRot(level.random.nextFloat() * 360.0F);
			spawned.getPersistentData().putBoolean("slr_job_change_advancement_knight", true);
		}
	}
}
