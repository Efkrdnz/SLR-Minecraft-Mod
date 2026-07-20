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

/** Creates instance-scoped return portals with a deterministic authored facing. */
public final class DungeonReturnPortalSpawner {
	private DungeonReturnPortalSpawner() {
	}

	@Nullable
	public static Entity spawn(ServerLevel level, BlockPos position, @Nullable Direction facing,
			UUID instanceId, String dungeonTag) {
		if (level == null || position == null || instanceId == null)
			return null;
		Entity portal = SololevelingModEntities.PORTAL_12.get().spawn(level, position, MobSpawnType.MOB_SUMMONED);
		if (portal == null)
			return null;
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
		portal.getPersistentData().putString("slr_dungeon_instance", instanceId.toString());
		portal.getPersistentData().putString("dungeon_tag",
				dungeonTag == null || dungeonTag.isBlank() ? instanceId.toString() : dungeonTag);
		return portal;
	}
}
