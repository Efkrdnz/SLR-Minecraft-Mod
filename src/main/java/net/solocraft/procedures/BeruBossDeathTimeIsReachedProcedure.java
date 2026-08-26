package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.util.AriseExtractionRules;
import net.solocraft.procedures.ShadowKillCreditHelper;
import net.solocraft.entity.BeruDeadBodyEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class BeruBossDeathTimeIsReachedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z,
			Entity defeated) {
		if (world instanceof ServerLevel _level) {
			Entity entityToSpawn = SololevelingModEntities.BERU_DEAD_BODY.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
			if (entityToSpawn != null) {
				if (entityToSpawn instanceof BeruDeadBodyEntity body)
					body.getEntityData().set(BeruDeadBodyEntity.DATA_tries,
							AriseExtractionRules.MAX_BOSS_EXTRACTION_FAILURES);
				Player creditedKiller = defeated instanceof LivingEntity living
						? ShadowKillCreditHelper.creditedPlayer(world,
								living.getKillCredit()) : null;
				if (creditedKiller != null)
					entityToSpawn.getPersistentData().putUUID(
							AriseExtractionRules.EXTRACTION_OWNER_TAG,
							creditedKiller.getUUID());
				double targetLevel = DungeonLevelHelper.levelOf(defeated);
				if (targetLevel > 0.0D)
					entityToSpawn.getPersistentData().putDouble(
							AriseExtractionRules.TARGET_LEVEL_TAG, targetLevel);
			}
		}
	}
}
