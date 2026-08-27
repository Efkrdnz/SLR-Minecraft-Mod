package net.solocraft.dungeon.runtime;

import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Creates return portals after installing their ownership metadata.
 *
 * <p>{@link net.minecraft.world.entity.EntityType#spawn} joins the entity to the
 * level before returning it to the caller. Return-portal recovery observes entity
 * joins, so adding the tags afterwards leaves a window where a portal looks
 * unowned. Creating and configuring it first keeps reconciliation idempotent.</p>
 */
public final class DungeonReturnPortalSpawner {
	private DungeonReturnPortalSpawner() {
	}

	@Nullable
	public static Entity spawn(ServerLevel level, BlockPos position, @Nullable Direction facing,
			UUID instanceId, String dungeonTag) {
		if (level == null || position == null || instanceId == null)
			return null;
		return spawnConfigured(level, position, facing, instanceId, dungeonTag);
	}

	@Nullable
	public static Entity spawnUnscoped(ServerLevel level, BlockPos position, String dungeonTag) {
		if (level == null || position == null)
			return null;
		return spawnConfigured(level, position, null, null, dungeonTag);
	}

	@Nullable
	private static Entity spawnConfigured(ServerLevel level, BlockPos position,
			@Nullable Direction facing, @Nullable UUID instanceId, String dungeonTag) {
		Entity portal = SololevelingModEntities.PORTAL_12.get().create(level);
		if (portal == null)
			return null;
		portal.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D,
				0.0F, 0.0F);
		if (portal instanceof Mob mob)
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(position),
					MobSpawnType.MOB_SUMMONED, null);
		if (facing != null && facing.getAxis().isHorizontal()) {
			float yaw = facing.toYRot();
			portal.setYRot(yaw);
			portal.setXRot(0.0F);
			portal.yRotO = yaw;
			portal.xRotO = 0.0F;
			if (portal instanceof Mob mob) {
				mob.yBodyRot = yaw;
				mob.yBodyRotO = yaw;
				mob.setYHeadRot(yaw);
				mob.yHeadRotO = yaw;
			}
		}
		if (instanceId != null)
			portal.getPersistentData().putString(DungeonMobLevelAdapter.INSTANCE_TAG,
					instanceId.toString());
		String cleanDungeonTag = dungeonTag == null ? "" : dungeonTag.trim();
		if (!cleanDungeonTag.isEmpty())
			portal.getPersistentData().putString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG,
					cleanDungeonTag);
		else if (instanceId != null)
			portal.getPersistentData().putString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG,
					instanceId.toString());
		if (!level.addFreshEntity(portal)) {
			portal.discard();
			return null;
		}
		return portal;
	}
}
