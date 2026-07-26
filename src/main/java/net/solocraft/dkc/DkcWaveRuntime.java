package net.solocraft.dkc;

import net.solocraft.procedures.DKCDemonSpawnerProcedure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

import java.util.UUID;

/**
 * Lightweight ownership, containment, and stuck recovery for persistent DKC
 * floor-wave mobs. Cheap checks run once per second; pathfinding is reserved
 * for a mob that has made no useful progress for ten seconds plus one staggered
 * safety audit every fifteen seconds.
 */
public final class DkcWaveRuntime {
	private static final String SAMPLE_VALID_TAG = "dkc_progress_sample_valid";
	private static final String SAMPLE_DISTANCE_TAG = "dkc_progress_sample_distance";
	private static final String STUCK_SAMPLES_TAG = "dkc_stuck_samples";
	private static final double MIN_PROGRESS_TOWARD_OWNER = 1.0D;
	private static final double MELEE_REACHABLE_SQUARED = 3.0D * 3.0D;
	private static final double CLOSE_TO_OWNER_SQUARED = 12.0D * 12.0D;

	private DkcWaveRuntime() {
	}

	public static void tick(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level))
			return;
		long gameTime = level.getGameTime();
		if (Math.floorMod(gameTime + mob.getId(), 20L) != 0L)
			return;
		CompoundTag tag = mob.getPersistentData();
		if (!DKCDemonSpawnerProcedure.FLOOR_WAVE_ROLE.equals(
				tag.getString(DKCDemonSpawnerProcedure.ROLE_TAG)))
			return;

		int floor = (int) tag.getDouble("dkc_floor_number");
		ServerPlayer owner = owner(level, tag.getString("dkc_spawned_by"));
		if (floor < 2 || floor > 19 || owner == null || owner.serverLevel() != level
				|| !DkcSpatialLayout.isPlayerInFloor(owner, floor)
				|| !DKCDemonSpawnerProcedure.isCurrentWaveMob(mob, owner, floor)
				|| owner.getPersistentData().getBoolean("dkc_floor_" + floor + "_complete")) {
			mob.discard();
			return;
		}

		if (mob.getTarget() != owner)
			mob.setTarget(owner);
		boolean ownerInsideCombatArea = DkcFloorBuilder.isInsideCombatArea(
				owner, floor, owner.blockPosition());
		if (!DkcFloorBuilder.isInsideCombatArea(owner, floor, mob.blockPosition())) {
			/*
			 * If the player deliberately leaves the authored combat footprint,
			 * demons are allowed to pursue them outside it. Leash recovery resumes
			 * only after the player returns, so enemies never blink away mid-fight.
			 */
			if (ownerInsideCombatArea)
				recoverOrDiscard(level, owner, floor, mob);
			else
				clearSamples(tag);
			return;
		}
		if (!ownerInsideCombatArea) {
			clearSamples(tag);
			return;
		}
		if (Math.floorMod(gameTime + mob.getId(), 100L) != 0L)
			return;

		tickProgress(level, owner, floor, mob, tag);
	}

	private static void tickProgress(ServerLevel level, ServerPlayer owner, int floor, Mob mob,
			CompoundTag tag) {
		double distanceSquared = mob.distanceToSqr(owner);
		double distance = Math.sqrt(distanceSquared);
		boolean scheduledAudit = Math.floorMod(level.getGameTime() + mob.getId(), 300L) == 0L;
		if (distanceSquared <= MELEE_REACHABLE_SQUARED && mob.hasLineOfSight(owner)) {
			storeSample(tag, distance, 0);
			return;
		}
		if (distanceSquared <= CLOSE_TO_OWNER_SQUARED) {
			storeSample(tag, distance, 0);
			if (!scheduledAudit)
				return;
		} else if (!tag.getBoolean(SAMPLE_VALID_TAG)) {
			storeSample(tag, distance, 0);
			if (!scheduledAudit)
				return;
		} else {
			double progressTowardOwner = tag.getDouble(SAMPLE_DISTANCE_TAG) - distance;
			int stuckSamples = progressTowardOwner < MIN_PROGRESS_TOWARD_OWNER
					? tag.getInt(STUCK_SAMPLES_TAG) + 1 : 0;
			storeSample(tag, distance, stuckSamples);
			if (stuckSamples < 2 && !scheduledAudit)
				return;
		}

		Path path = mob.getNavigation().createPath(owner.blockPosition(), 0);
		if (path != null && path.canReach()) {
			storeSample(tag, distance, 0);
			return;
		}
		recoverOrDiscard(level, owner, floor, mob);
	}

	private static void recoverOrDiscard(ServerLevel level, ServerPlayer owner, int floor, Mob mob) {
		clearSamples(mob.getPersistentData());
		if (!DkcFloorBuilder.recoverWaveMob(level, owner, floor, mob))
			mob.discard();
	}

	private static void storeSample(CompoundTag tag, double distance, int stuckSamples) {
		tag.putBoolean(SAMPLE_VALID_TAG, true);
		tag.putDouble(SAMPLE_DISTANCE_TAG, distance);
		tag.putInt(STUCK_SAMPLES_TAG, Math.max(0, stuckSamples));
	}

	private static void clearSamples(CompoundTag tag) {
		tag.remove(SAMPLE_VALID_TAG);
		tag.remove(SAMPLE_DISTANCE_TAG);
		tag.remove(STUCK_SAMPLES_TAG);
	}

	private static ServerPlayer owner(ServerLevel level, String ownerText) {
		try {
			return level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerText));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}
}
