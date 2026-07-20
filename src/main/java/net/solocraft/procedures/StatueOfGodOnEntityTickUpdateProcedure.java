package net.solocraft.procedures;

import net.solocraft.entity.StatueOfGodEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(world instanceof ServerLevel level) || !(entity instanceof StatueOfGodEntity statue) || !statue.isAlive())
			return;

		CompoundTag data = statue.getPersistentData();
		String state = data.getString("state");
		if (!state.isEmpty() && !state.equals(statue.getEntityData().get(StatueOfGodEntity.DATA_state)))
			statue.getEntityData().set(StatueOfGodEntity.DATA_state, state);
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

	private static void tickWaking(ServerLevel level, StatueOfGodEntity statue, CompoundTag data) {
		statue.setNoAi(true);
		statue.getNavigation().stop();
		statue.setDeltaMovement(Vec3.ZERO);
		statue.fallDistance = 0.0F;

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

	private static void activate(StatueOfGodEntity statue, ServerPlayer target) {
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
