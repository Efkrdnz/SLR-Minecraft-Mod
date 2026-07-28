package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;
import net.solocraft.entity.StatueOfGodEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class StatueOfGodOnEntityTickUpdateProcedure {
	private static final String AGGRESSIVE_STATE = "aggresive";
	private static final String WAKING_STATE = "waking";
	private static final String THRONE_STATE = "throne";
	private static final int PLAYER_SCAN_INTERVAL = 10;
	// "standing and smiling" lasts 3.5417 seconds, or just under 71 ticks.
	private static final int WAKE_ANIMATION_TICKS = 72;
	private static final double CHASE_SPEED = 1.2D;
	private static final double DIRECT_CHASE_SPEED = 0.22D;
	private static final double STOP_CHASING_DISTANCE_SQR = 6.0D * 6.0D;
	private static final double ACTIVATION_RANGE_SQR = 32.0D * 32.0D;
	private static final double LEASH_RANGE_SQR = 96.0D * 96.0D;
	private static final double STORY_ACTIVATION_RANGE_SQR = 128.0D * 128.0D;
	private static final double STORY_TARGET_SCAN_RANGE = 256.0D;
	private static final double STORY_MELEE_RANGE = 2.0D;
	private static final int STORY_MELEE_COOLDOWN_TICKS = 20;
	private static final String STORY_STATUE_TAG = "slr_story_intro_statue";
	private static final String STORY_INSTANCE_TAG = "slr_story_intro_instance";
	private static final String STORY_OWNER_TAG = "slr_story_intro_owner";
	private static final String STORY_LASER_DONE_TAG = "slr_story_intro_laser_done";
	private static final String STORY_ACTIVATION_AT_TAG = "slr_story_intro_activation_at";
	private static final String STORY_HUNTER_TAG = "slr_story_intro_hunter";
	private static final String STORY_NEXT_MELEE_AT_TAG =
			"slr_story_intro_next_melee_at";

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof StatueOfGodEntity statue) || !statue.isAlive())
			return;

		CompoundTag data = statue.getPersistentData();
		String state = data.getString("state");
		if (!state.isEmpty() && !state.equals(statue.getEntityData().get(StatueOfGodEntity.DATA_state)))
			statue.getEntityData().set(StatueOfGodEntity.DATA_state, state);
		if (data.getBoolean(STORY_STATUE_TAG)) {
			tickStoryIntro(level, statue, data, state);
			return;
		}
		if (WAKING_STATE.equals(state)) {
			tickWaking(level, statue, data);
			return;
		}

		if (!AGGRESSIVE_STATE.equals(state)) {
			if (!statue.isNoAi())
				statue.setNoAi(true);
			if (!shouldScan(statue))
				return;

			ServerPlayer player = findNearestPlayer(level, statue, ACTIVATION_RANGE_SQR, true);
			if (player != null)
				activate(statue, player);
			return;
		}

		if (statue.isNoAi())
			statue.setNoAi(false);

		if (shouldScan(statue)) {
			ServerPlayer nearest = findNearestPlayer(level, statue, LEASH_RANGE_SQR, false);
			if (nearest == null) {
				resetToThrone(statue);
				return;
			}
			LivingEntity currentTarget = statue.getTarget();
			if (!isValidPlayer(currentTarget) || statue.distanceToSqr(currentTarget) > LEASH_RANGE_SQR)
				statue.setTarget(nearest);
			statue.getNavigation().moveTo(statue.getTarget(), CHASE_SPEED);
		}

		LivingEntity target = statue.getTarget();
		if (isValidPlayer(target)) {
			statue.faceTarget(target);
			chaseTarget(statue, target);
		}
	}

	private static void tickStoryIntro(ServerLevel level, StatueOfGodEntity statue, CompoundTag data, String state) {
		boolean activationReady = isStoryActivationReady(level, data);
		if (!activationReady && (WAKING_STATE.equals(state) || AGGRESSIVE_STATE.equals(state))) {
			resetToThrone(statue);
			return;
		}

		if (WAKING_STATE.equals(state)) {
			tickStoryWaking(level, statue, data);
			return;
		}

		if (!AGGRESSIVE_STATE.equals(state)) {
			holdStill(statue);
			if (!activationReady || !shouldScan(statue))
				return;

			ServerPlayer owner = findStoryOwner(level, data);
			if (owner != null && owner.isShiftKeyDown()
					&& statue.distanceToSqr(owner) <= STORY_ACTIVATION_RANGE_SQR) {
				LivingEntity target = findStoryTarget(level, statue, data);
				if (target != null)
					activate(statue, target);
			}
			return;
		}

		LivingEntity target = findStoryTarget(level, statue, data);
		if (target == null) {
			resetToThrone(statue);
			return;
		}

		if (statue.isNoAi())
			statue.setNoAi(false);
		if (statue.getTarget() != target)
			statue.setTarget(target);
		statue.faceTarget(target);
		chaseStoryTarget(statue, target);
	}

	private static void tickStoryWaking(ServerLevel level, StatueOfGodEntity statue, CompoundTag data) {
		holdStill(statue);
		if ("empty".equals(statue.animationprocedure)
				&& "undefined".equals(statue.getSyncedAnimation()))
			statue.setAnimation("standing and smiling");

		LivingEntity target = findStoryTarget(level, statue, data);
		if (target == null) {
			resetToThrone(statue);
			return;
		}
		if (statue.getTarget() != target)
			statue.setTarget(target);
		statue.faceTarget(target);

		int wakeTicks = data.getInt("IA") + 1;
		data.putInt("IA", wakeTicks);
		if (wakeTicks < WAKE_ANIMATION_TICKS)
			return;

		statue.getEntityData().set(StatueOfGodEntity.DATA_story_upright, true);
		data.putString("state", AGGRESSIVE_STATE);
		data.putInt("IA", 0);
		statue.getEntityData().set(StatueOfGodEntity.DATA_state, AGGRESSIVE_STATE);
		statue.setNoAi(false);
		statue.setTarget(target);
		chaseStoryTarget(statue, target);
	}

	private static boolean isStoryActivationReady(ServerLevel level, CompoundTag data) {
		return data.getBoolean(STORY_LASER_DONE_TAG)
				&& data.contains(STORY_ACTIVATION_AT_TAG, Tag.TAG_LONG)
				&& level.getGameTime() >= data.getLong(STORY_ACTIVATION_AT_TAG);
	}

	private static LivingEntity findStoryTarget(ServerLevel level, StatueOfGodEntity statue, CompoundTag data) {
		LivingEntity currentTarget = statue.getTarget();
		int instance = data.getInt(STORY_INSTANCE_TAG);
		if (currentTarget instanceof HunterEntity currentHunter
				&& isMatchingStoryHunter(currentHunter, instance))
			return currentHunter;

		HunterEntity nearestHunter = null;
		double nearestDistance = Double.MAX_VALUE;
		for (HunterEntity hunter : level.getEntitiesOfClass(
				HunterEntity.class,
				statue.getBoundingBox().inflate(STORY_TARGET_SCAN_RANGE),
				candidate -> isMatchingStoryHunter(candidate, instance))) {
			double distance = statue.distanceToSqr(hunter);
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearestHunter = hunter;
			}
		}
		if (nearestHunter != null)
			return nearestHunter;
		return findStoryOwner(level, data);
	}

	private static void chaseStoryTarget(StatueOfGodEntity statue,
			LivingEntity target) {
		if (!statue.getNavigation().isDone())
			statue.getNavigation().stop();
		statue.faceTarget(target);

		Vec3 movement = statue.getDeltaMovement();
		double surfaceDistance = CombatRangeHelper.horizontalSurfaceDistance(
				statue, target);
		if (surfaceDistance <= STORY_MELEE_RANGE) {
			statue.setDeltaMovement(movement.x * 0.2D, movement.y(),
					movement.z * 0.2D);
			tryStoryMeleeAttack(statue, target);
			return;
		}

		double dx = target.getX() - statue.getX();
		double dz = target.getZ() - statue.getZ();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		if (horizontalDistance <= 1.0E-5D)
			return;
		statue.setDeltaMovement(
				dx / horizontalDistance * DIRECT_CHASE_SPEED,
				movement.y(),
				dz / horizontalDistance * DIRECT_CHASE_SPEED);
		statue.hasImpulse = true;
	}

	private static void tryStoryMeleeAttack(StatueOfGodEntity statue,
			LivingEntity target) {
		CompoundTag data = statue.getPersistentData();
		long gameTime = statue.level().getGameTime();
		if (gameTime < data.getLong(STORY_NEXT_MELEE_AT_TAG))
			return;
		data.putLong(STORY_NEXT_MELEE_AT_TAG,
				gameTime + STORY_MELEE_COOLDOWN_TICKS);
		statue.swing(InteractionHand.MAIN_HAND);
		statue.doHurtTarget(target);
	}

	private static boolean isMatchingStoryHunter(HunterEntity hunter, int instance) {
		CompoundTag hunterData = hunter.getPersistentData();
		return hunter.isAlive() && !hunter.isRemoved()
				&& hunterData.getBoolean(STORY_HUNTER_TAG)
				&& hunterData.getInt(STORY_INSTANCE_TAG) == instance;
	}

	private static ServerPlayer findStoryOwner(ServerLevel level, CompoundTag data) {
		if (!data.hasUUID(STORY_OWNER_TAG))
			return null;
		if (level.getPlayerByUUID(data.getUUID(STORY_OWNER_TAG)) instanceof ServerPlayer owner
				&& isValidPlayer(owner))
			return owner;
		return null;
	}

	private static void tickWaking(ServerLevel level, StatueOfGodEntity statue, CompoundTag data) {
		holdStill(statue);

		LivingEntity target = statue.getTarget();
		if (!isValidPlayer(target) || statue.distanceToSqr(target) > LEASH_RANGE_SQR) {
			target = findNearestPlayer(level, statue, LEASH_RANGE_SQR, false);
			if (target == null) {
				resetToThrone(statue);
				return;
			}
			statue.setTarget(target);
		}
		statue.faceTarget(target);

		int wakeTicks = data.getInt("IA") + 1;
		data.putInt("IA", wakeTicks);
		if (wakeTicks < WAKE_ANIMATION_TICKS)
			return;

		data.putString("state", AGGRESSIVE_STATE);
		data.putInt("IA", 0);
		statue.getEntityData().set(StatueOfGodEntity.DATA_state, AGGRESSIVE_STATE);
		statue.setNoAi(false);
		statue.setTarget(target);
		statue.getNavigation().moveTo(target, CHASE_SPEED);
		chaseTarget(statue, target);
	}

	private static void holdStill(StatueOfGodEntity statue) {
		statue.setNoAi(true);
		statue.getNavigation().stop();
		statue.setDeltaMovement(Vec3.ZERO);
		statue.fallDistance = 0.0F;
	}

	private static void chaseTarget(StatueOfGodEntity statue, LivingEntity target) {
		double dx = target.getX() - statue.getX();
		double dz = target.getZ() - statue.getZ();
		double horizontalDistanceSqr = dx * dx + dz * dz;
		if (horizontalDistanceSqr <= STOP_CHASING_DISTANCE_SQR)
			return;

		statue.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), CHASE_SPEED);
		Vec3 movement = statue.getDeltaMovement();
		if (movement.horizontalDistanceSqr() > 1.0E-5D)
			return;

		// The Statue's 5.25 x 23.25 hitbox can make vanilla path creation fail on
		// the throne steps. Supply a small direct impulse only while navigation is
		// stalled; normal entity collision still prevents it walking through walls.
		double horizontalDistance = Math.sqrt(horizontalDistanceSqr);
		statue.setDeltaMovement(dx / horizontalDistance * DIRECT_CHASE_SPEED, movement.y(),
				dz / horizontalDistance * DIRECT_CHASE_SPEED);
	}

	private static boolean shouldScan(StatueOfGodEntity statue) {
		return Math.floorMod(statue.tickCount + statue.getId(), PLAYER_SCAN_INTERVAL) == 0;
	}

	private static ServerPlayer findNearestPlayer(ServerLevel level, StatueOfGodEntity statue, double rangeSqr,
			boolean requireBowing) {
		ServerPlayer nearest = null;
		double nearestDistance = rangeSqr;
		for (ServerPlayer player : level.players()) {
			if (!isValidPlayer(player) || requireBowing && !player.isShiftKeyDown())
				continue;
			double distance = statue.distanceToSqr(player);
			if (distance <= nearestDistance) {
				nearestDistance = distance;
				nearest = player;
			}
		}
		return nearest;
	}

	private static boolean isValidPlayer(LivingEntity entity) {
		return entity instanceof ServerPlayer player && player.isAlive() && !player.isCreative() && !player.isSpectator();
	}

	private static void activate(StatueOfGodEntity statue, LivingEntity target) {
		statue.getEntityData().set(StatueOfGodEntity.DATA_story_upright, false);
		statue.getPersistentData().remove(STORY_NEXT_MELEE_AT_TAG);
		statue.getPersistentData().putString("state", WAKING_STATE);
		statue.getPersistentData().putInt("IA", 0);
		statue.getEntityData().set(StatueOfGodEntity.DATA_state, WAKING_STATE);
		statue.setNoAi(true);
		statue.setTarget(target);
		statue.faceTarget(target);
		statue.getNavigation().stop();
		statue.setDeltaMovement(Vec3.ZERO);
		statue.setAnimation("standing and smiling");
	}

	private static void resetToThrone(StatueOfGodEntity statue) {
		CompoundTag data = statue.getPersistentData();
		statue.getEntityData().set(StatueOfGodEntity.DATA_story_upright, false);
		data.remove(STORY_NEXT_MELEE_AT_TAG);
		data.putString("state", THRONE_STATE);
		data.putInt("IA", 0);
		statue.getEntityData().set(StatueOfGodEntity.DATA_state, THRONE_STATE);
		statue.setNoAi(true);
		statue.setTarget(null);
		statue.getNavigation().stop();
		statue.setDeltaMovement(Vec3.ZERO);
		statue.fallDistance = 0.0F;

		int homeX = statue.getEntityData().get(StatueOfGodEntity.DATA_default_x);
		int homeY = statue.getEntityData().get(StatueOfGodEntity.DATA_default_y);
		int homeZ = statue.getEntityData().get(StatueOfGodEntity.DATA_default_z);
		statue.teleportTo(homeX + 0.5D, homeY, homeZ + 0.5D);
		statue.setXRot(0.0F);
		statue.xRotO = 0.0F;
		statue.faceYaw(data.contains("CartenonHomeYaw") ? data.getFloat("CartenonHomeYaw") : 180.0F);
	}
}
