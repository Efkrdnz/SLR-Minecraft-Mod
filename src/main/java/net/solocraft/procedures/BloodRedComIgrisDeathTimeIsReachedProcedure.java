package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.util.AriseExtractionRules;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.procedures.ShadowKillCreditHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class BloodRedComIgrisDeathTimeIsReachedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level) {
			Entity entityToSpawn = SololevelingModEntities.IGRIS_DEAD_BODY.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			if (entityToSpawn != null) {
				JobChangeQuestManager.copyAttempt(entity, entityToSpawn);
				Player creditedKiller = entity instanceof LivingEntity living
						? ShadowKillCreditHelper.creditedPlayer(world,
								living.getKillCredit()) : null;
				if (creditedKiller != null)
					entityToSpawn.getPersistentData().putUUID(
							AriseExtractionRules.EXTRACTION_OWNER_TAG,
							creditedKiller.getUUID());
				else if (JobChangeQuestManager.attemptOwner(entity) != null)
					entityToSpawn.getPersistentData().putUUID(
							AriseExtractionRules.EXTRACTION_OWNER_TAG,
							JobChangeQuestManager.attemptOwner(entity));
				double targetLevel = DungeonLevelHelper.levelOf(entity);
				if (targetLevel > 0.0D)
					entityToSpawn.getPersistentData().putDouble(
							AriseExtractionRules.TARGET_LEVEL_TAG, targetLevel);
				entityToSpawn.setYRot(entity.getYRot());
				entityToSpawn.setYBodyRot(entity.getYRot());
				entityToSpawn.setYHeadRot(entity.getYRot());
				entityToSpawn.setXRot(entity.getXRot());
			}
		}
	}
}
