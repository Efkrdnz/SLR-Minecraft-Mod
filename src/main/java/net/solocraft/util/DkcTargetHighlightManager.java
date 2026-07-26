package net.solocraft.util;

import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.procedures.DKCDemonSpawnerProcedure;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Refreshes short private leases for the targets belonging to one DKC player. */
public final class DkcTargetHighlightManager {
	private static final String SOURCE = "dkc:floor_targets";
	private static final int REFRESH_INTERVAL = 20;
	private static final int LEASE_DURATION = 50;

	private DkcTargetHighlightManager() {
	}

	public static void sync(ServerPlayer player, int floor) {
		if (player == null || floor < 1 || floor > 20
				|| player.serverLevel().getGameTime() % REFRESH_INTERVAL != 0L)
			return;
		ServerLevel level = player.serverLevel();
		AABB floorBounds = DkcFloorBuilder.combatBounds(player, floor);
		String ownerId = player.getStringUUID();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, floorBounds,
				entity -> belongsToPlayerFloor(entity, ownerId, floor))) {
			boolean boss = isBoss(target);
			boolean elite = target instanceof DemonKnightEntity
					|| target.getPersistentData().getBoolean(DKCDemonSpawnerProcedure.MINIBOSS_TAG);
			int color = boss ? EntityHighlightSystem.COLOR_WAVE_BOSS
					: elite ? EntityHighlightSystem.COLOR_WAVE_ELITE
					: EntityHighlightSystem.COLOR_PERCEPTION_HOSTILE;
			int priority = boss ? EntityHighlightSystem.PRIORITY_DUNGEON_BOSS
					: elite ? EntityHighlightSystem.PRIORITY_DUNGEON_ELITE
					: EntityHighlightSystem.PRIORITY_DUNGEON_NORMAL;
			EntityHighlightSystem.show(player, target, SOURCE, color, LEASE_DURATION, priority);
		}
	}

	private static boolean belongsToPlayerFloor(LivingEntity target, String ownerId, int floor) {
		String targetOwner = target.getPersistentData().getString("dkc_spawned_by");
		int targetFloor = (int) target.getPersistentData().getDouble("dkc_floor_number");
		return ownerId.equals(targetOwner) && targetFloor == floor;
	}

	private static boolean isBoss(LivingEntity target) {
		return target instanceof CerberusEntity || target instanceof VulcanEntity
				|| target instanceof BaranEntity || target instanceof KaiselinEntity;
	}
}
