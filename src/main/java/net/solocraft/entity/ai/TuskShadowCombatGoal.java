package net.solocraft.entity.ai;

import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.util.CombatRangeHelper;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.TuskShadowCombatManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Grounded ranged movement for Shadow Tusk.
 *
 * <p>The shared command coordinator remains responsible for selecting a target.
 * This goal only turns that target into purposeful movement: acquire a firing
 * lane, hold range, retreat from pressure, and recover from stalled paths.</p>
 */
public final class TuskShadowCombatGoal extends Goal {
	private static final double APPROACH_SPEED = 1.18D;
	private static final double REPOSITION_SPEED = 1.10D;
	private static final double RETREAT_SPEED = 1.26D;
	private static final int PATH_INTERVAL_TICKS = 9;
	private static final int REPOSITION_MIN_TICKS = 30;
	private static final int REPOSITION_VARIANCE_TICKS = 31;

	private final TuskShadowEntity tusk;
	private Vec3 lastPosition = Vec3.ZERO;
	private double lastTargetDistanceSqr = Double.POSITIVE_INFINITY;
	private long nextPathTick;
	private long nextRepositionTick;
	private int stalledTicks;
	private boolean regrouping;
	private TuskShadowCombatPolicy.RecoveryStage handledRecoveryStage =
			TuskShadowCombatPolicy.RecoveryStage.NONE;

	public TuskShadowCombatGoal(TuskShadowEntity tusk) {
		this.tusk = tusk;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return isSafeTarget(tusk.getTarget());
	}

	@Override
	public boolean canContinueToUse() {
		return isSafeTarget(tusk.getTarget())
				|| TuskShadowCombatManager.isCasting(tusk);
	}

	@Override
	public void start() {
		tusk.setAggressive(true);
		lastPosition = tusk.position();
		LivingEntity target = tusk.getTarget();
		lastTargetDistanceSqr = target == null
				? Double.POSITIVE_INFINITY : tusk.distanceToSqr(target);
		stalledTicks = 0;
		regrouping = false;
		handledRecoveryStage = TuskShadowCombatPolicy.RecoveryStage.NONE;
		nextPathTick = 0L;
		nextRepositionTick = tusk.level().getGameTime()
				+ REPOSITION_MIN_TICKS
				+ tusk.getRandom().nextInt(REPOSITION_VARIANCE_TICKS);
	}

	@Override
	public void stop() {
		tusk.setAggressive(false);
		tusk.getNavigation().stop();
		stalledTicks = 0;
		regrouping = false;
		handledRecoveryStage = TuskShadowCombatPolicy.RecoveryStage.NONE;
		if (!TuskShadowCombatManager.isCasting(tusk))
			tusk.setCombatState("idle");
	}

	@Override
	public void tick() {
		LivingEntity target = tusk.getTarget();
		if (TuskShadowCombatManager.isCasting(tusk)) {
			tusk.getNavigation().stop();
			if (target != null)
				resetProgress(target);
			return;
		}
		if (!isSafeTarget(target))
			return;

		tusk.getLookControl().setLookAt(target, 45.0F, 45.0F);

		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(tusk);
		if (owner != null && tusk.distanceToSqr(owner)
				> square(TuskShadowCombatPolicy.OWNER_LEASH_RANGE)) {
			boolean enteringRegroup = !regrouping;
			regrouping = true;
			tusk.setCombatState("regrouping");
			requestDirectPath(owner, APPROACH_SPEED, enteringRegroup);
			trackProgress(target, true);
			return;
		}
		regrouping = false;

		double surfaceDistance = CombatRangeHelper.surfaceDistance(tusk, target);
		boolean hasLineOfSight = tusk.getSensing().hasLineOfSight(target);
		boolean expectsMovement;

		if (TuskShadowCombatPolicy.shouldRetreat(surfaceDistance)) {
			tusk.setCombatState("retreating");
			expectsMovement = retreatFrom(target);
		} else if (TuskShadowCombatPolicy.shouldApproach(surfaceDistance,
				hasLineOfSight)) {
			tusk.setCombatState(hasLineOfSight ? "approaching" : "seeking_line");
			expectsMovement = seekCastingLane(target, !hasLineOfSight);
		} else {
			expectsMovement = holdAndReposition(target);
		}

		if (tusk.isInWaterOrBubble() && expectsMovement
				&& (tusk.tickCount & 3) == 0)
			tusk.getJumpControl().jump();
		trackProgress(target, expectsMovement);
	}

	private boolean holdAndReposition(LivingEntity target) {
		long now = tusk.level().getGameTime();
		if (now < nextRepositionTick) {
			if (tusk.getNavigation().isDone())
				tusk.setCombatState("holding");
			return !tusk.getNavigation().isDone();
		}

		nextRepositionTick = now + REPOSITION_MIN_TICKS
				+ tusk.getRandom().nextInt(REPOSITION_VARIANCE_TICKS);
		if (tryLateralPath(target, REPOSITION_SPEED)) {
			tusk.setCombatState("repositioning");
			return true;
		}
		tusk.getNavigation().stop();
		tusk.setCombatState("holding");
		return false;
	}

	private boolean seekCastingLane(LivingEntity target, boolean preferSide) {
		long now = tusk.level().getGameTime();
		if (now < nextPathTick && !tusk.getNavigation().isDone())
			return true;
		nextPathTick = now + PATH_INTERVAL_TICKS
				+ Math.floorMod(tusk.getId(), 3);
		if (preferSide && tryCastingLane(target))
			return true;
		return tusk.getNavigation().moveTo(target, APPROACH_SPEED);
	}

	private void requestDirectPath(LivingEntity destination, double speed,
			boolean force) {
		long now = tusk.level().getGameTime();
		if (!force && now < nextPathTick && !tusk.getNavigation().isDone())
			return;
		nextPathTick = now + PATH_INTERVAL_TICKS
				+ Math.floorMod(tusk.getId(), 3);
		tusk.getNavigation().moveTo(destination, speed);
	}

	private boolean retreatFrom(LivingEntity target) {
		long now = tusk.level().getGameTime();
		if (now < nextPathTick && !tusk.getNavigation().isDone())
			return true;
		nextPathTick = now + PATH_INTERVAL_TICKS;
		Vec3 retreat = DefaultRandomPos.getPosAway(tusk, 10, 5,
				target.position());
		if (retreat != null
				&& retreat.distanceToSqr(target.position())
						> tusk.distanceToSqr(target)
				&& tryPathTo(retreat, RETREAT_SPEED))
			return true;

		Vec3 away = tusk.position().subtract(target.position())
				.multiply(1.0D, 0.0D, 1.0D);
		if (away.lengthSqr() > 1.0E-5D) {
			away = away.normalize().scale(0.16D);
			tusk.setDeltaMovement(tusk.getDeltaMovement()
					.add(away.x, 0.0D, away.z));
			tusk.hasImpulse = true;
			return true;
		}
		return false;
	}

	private boolean tryCastingLane(LivingEntity target) {
		Vec3 away = tusk.position().subtract(target.position())
				.multiply(1.0D, 0.0D, 1.0D);
		if (away.lengthSqr() < 1.0E-4D)
			away = new Vec3(0.0D, 0.0D, 1.0D);
		else
			away = away.normalize();
		Vec3 side = new Vec3(-away.z, 0.0D, away.x);
		if ((tusk.getId() & 1) != 0)
			side = side.scale(-1.0D);
		double y = tusk.getY();
		Vec3 base = target.position().add(away.scale(14.0D));
		Vec3[] candidates = {
				new Vec3(base.x + side.x * 5.0D, y, base.z + side.z * 5.0D),
				new Vec3(base.x - side.x * 5.0D, y, base.z - side.z * 5.0D),
				new Vec3(base.x + side.x * 8.0D, y, base.z + side.z * 8.0D),
				new Vec3(base.x - side.x * 8.0D, y, base.z - side.z * 8.0D),
				new Vec3(base.x, y, base.z)
		};
		for (Vec3 candidate : candidates) {
			if (tryPathTo(candidate, APPROACH_SPEED))
				return true;
		}
		return false;
	}

	private boolean tryLateralPath(LivingEntity target, double speed) {
		Vec3 toward = target.position().subtract(tusk.position())
				.multiply(1.0D, 0.0D, 1.0D);
		if (toward.lengthSqr() < 1.0E-4D)
			return false;
		toward = toward.normalize();
		Vec3 side = new Vec3(-toward.z, 0.0D, toward.x);
		if (tusk.getRandom().nextBoolean())
			side = side.scale(-1.0D);
		Vec3 first = tusk.position().add(side.scale(4.5D));
		Vec3 second = tusk.position().add(side.scale(-4.5D));
		return tryPathTo(first, speed) || tryPathTo(second, speed);
	}

	private boolean tryPathTo(Vec3 destination, double speed) {
		BlockPos blockPos = BlockPos.containing(destination);
		if (!tusk.level().hasChunkAt(blockPos)
				|| !tusk.level().getWorldBorder().isWithinBounds(blockPos))
			return false;
		Path path = tusk.getNavigation().createPath(blockPos, 1);
		return path != null && path.canReach()
				&& tusk.getNavigation().moveTo(path, speed);
	}

	private void trackProgress(LivingEntity target, boolean expectsMovement) {
		double movedSqr = tusk.position().distanceToSqr(lastPosition);
		double targetDistanceSqr = tusk.distanceToSqr(target);
		lastPosition = tusk.position();
		if (!expectsMovement) {
			stalledTicks = 0;
			handledRecoveryStage = TuskShadowCombatPolicy.RecoveryStage.NONE;
			lastTargetDistanceSqr = targetDistanceSqr;
			return;
		}

		boolean progressed = movedSqr > 0.025D
				|| targetDistanceSqr + 0.75D < lastTargetDistanceSqr;
		lastTargetDistanceSqr = targetDistanceSqr;
		if (progressed) {
			stalledTicks = Math.max(0, stalledTicks - 3);
			if (stalledTicks == 0)
				handledRecoveryStage = TuskShadowCombatPolicy.RecoveryStage.NONE;
			return;
		}
		stalledTicks++;
		TuskShadowCombatPolicy.RecoveryStage stage =
				TuskShadowCombatPolicy.recoveryStage(stalledTicks);
		if (stage == handledRecoveryStage)
			return;
		handledRecoveryStage = stage;
		handleRecovery(stage, target);
	}

	private void handleRecovery(TuskShadowCombatPolicy.RecoveryStage stage,
			LivingEntity target) {
		switch (stage) {
			case REPATH -> {
				tusk.setCombatState("recovering_path");
				tusk.getNavigation().stop();
				nextPathTick = 0L;
				if (!tryCastingLane(target))
					tusk.getNavigation().moveTo(target, APPROACH_SPEED);
			}
			case ESCAPE -> {
				tusk.setCombatState("recovering_terrain");
				tusk.getJumpControl().jump();
				tusk.getNavigation().stop();
				nextPathTick = 0L;
				if (!tryLateralPath(target, RETREAT_SPEED))
					tusk.getNavigation().moveTo(target, APPROACH_SPEED);
			}
			case RECALL -> {
				tusk.setCombatState("recovering_owner");
				if (ShadowMonarchManager.tryRecoverStuckShadowNearOwner(tusk,
						target))
					resetProgress(target);
				else {
					stalledTicks = TuskShadowCombatPolicy.REPATH_STALLED_TICKS;
					handledRecoveryStage =
							TuskShadowCombatPolicy.RecoveryStage.REPATH;
					nextPathTick = 0L;
				}
			}
			case NONE -> {
			}
		}
	}

	private void resetProgress(LivingEntity target) {
		lastPosition = tusk.position();
		lastTargetDistanceSqr = tusk.distanceToSqr(target);
		stalledTicks = 0;
		handledRecoveryStage = TuskShadowCombatPolicy.RecoveryStage.NONE;
	}

	private boolean isSafeTarget(LivingEntity target) {
		return target != null && target.level() == tusk.level()
				&& ShadowMonarchManager.canShadowDamage(tusk, target);
	}

	private static double square(double value) {
		return value * value;
	}
}
