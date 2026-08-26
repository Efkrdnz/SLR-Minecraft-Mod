package net.solocraft.entity.ai;

import net.solocraft.entity.ShadowIronEntity;
import net.solocraft.util.CombatRangeHelper;
import net.solocraft.util.ShadowIronCombatManager;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Grounded guardian movement and action selection for Shadow Iron. */
public final class ShadowIronCombatGoal extends Goal {
	private static final double APPROACH_SPEED = 1.16D;
	private static final double INTERCEPT_SPEED = 1.28D;
	private static final int PATH_INTERVAL_TICKS = 8;

	private final ShadowIronEntity iron;
	private Vec3 lastPosition = Vec3.ZERO;
	private double lastTargetDistanceSqr = Double.POSITIVE_INFINITY;
	private long nextPathTick;
	private int stalledTicks;
	private ShadowIronCombatPolicy.RecoveryStage handledRecovery =
			ShadowIronCombatPolicy.RecoveryStage.NONE;

	public ShadowIronCombatGoal(ShadowIronEntity iron) {
		this.iron = iron;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		return iron.isActing() || isSafeTarget(iron.getTarget())
				|| ShadowIronCombatManager.findGuardianThreat(iron) != null;
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public void start() {
		iron.setAggressive(true);
		resetProgress(objective());
		nextPathTick = 0L;
	}

	@Override
	public void stop() {
		iron.setAggressive(false);
		iron.getNavigation().stop();
		stalledTicks = 0;
		handledRecovery = ShadowIronCombatPolicy.RecoveryStage.NONE;
	}

	@Override
	public void tick() {
		if (iron.isActing()) {
			iron.getNavigation().stop();
			LivingEntity current = objective();
			if (current != null)
				iron.getLookControl().setLookAt(current, 45.0F, 45.0F);
			return;
		}

		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		if (owner == null)
			return;
		if (iron.distanceToSqr(owner) > square(
				ShadowIronCombatPolicy.OWNER_LEASH_RANGE)) {
			requestPath(owner, INTERCEPT_SPEED, true);
			trackProgress(owner, true);
			return;
		}

		LivingEntity target = objective();
		if (!isSafeTarget(target))
			return;
		if (iron.getTarget() != target)
			iron.setTarget(target);
		iron.getLookControl().setLookAt(target, 45.0F, 45.0F);

		if (Math.floorMod(iron.tickCount + iron.getId(), 10) == 0)
			ShadowIronCombatManager.tryGuardianChallenge(iron, target);
		if (iron.canRoarNow() && ShadowIronCombatManager.shouldRoar(iron)
				&& iron.beginRoar()) {
			iron.getNavigation().stop();
			return;
		}
		if (iron.canBlockNow()
				&& ShadowIronCombatManager.shouldBrace(iron, target)
				&& iron.beginBlock(false)) {
			iron.getNavigation().stop();
			return;
		}

		double surfaceDistance = CombatRangeHelper.surfaceDistance(iron, target);
		if (surfaceDistance <= ShadowIronCombatPolicy.MELEE_REACH
				&& iron.getSensing().hasLineOfSight(target)) {
			iron.getNavigation().stop();
			if (iron.canAttackNow())
				iron.beginAttack(target, false);
			trackProgress(target, false);
			return;
		}

		requestPath(target, APPROACH_SPEED, false);
		if (iron.isInWaterOrBubble() && (iron.tickCount & 3) == 0)
			iron.getJumpControl().jump();
		trackProgress(target, true);
	}

	private LivingEntity objective() {
		LivingEntity target = iron.getTarget();
		return isSafeTarget(target) ? target
				: ShadowIronCombatManager.findGuardianThreat(iron);
	}

	private void requestPath(LivingEntity target, double speed, boolean force) {
		long now = iron.level().getGameTime();
		if (!force && now < nextPathTick && !iron.getNavigation().isDone())
			return;
		nextPathTick = now + PATH_INTERVAL_TICKS
				+ Math.floorMod(iron.getId(), 3);
		iron.getNavigation().moveTo(target, speed);
	}

	private void trackProgress(LivingEntity target, boolean expectsMovement) {
		if (target == null)
			return;
		double movedSqr = iron.position().distanceToSqr(lastPosition);
		double targetDistanceSqr = iron.distanceToSqr(target);
		lastPosition = iron.position();
		if (!expectsMovement) {
			stalledTicks = 0;
			handledRecovery = ShadowIronCombatPolicy.RecoveryStage.NONE;
			lastTargetDistanceSqr = targetDistanceSqr;
			return;
		}
		boolean progressed = movedSqr > 0.025D
				|| targetDistanceSqr + 0.75D < lastTargetDistanceSqr;
		lastTargetDistanceSqr = targetDistanceSqr;
		if (progressed) {
			stalledTicks = Math.max(0, stalledTicks - 3);
			if (stalledTicks == 0)
				handledRecovery = ShadowIronCombatPolicy.RecoveryStage.NONE;
			return;
		}
		stalledTicks++;
		ShadowIronCombatPolicy.RecoveryStage stage =
				ShadowIronCombatPolicy.recoveryStage(stalledTicks);
		if (stage == handledRecovery)
			return;
		handledRecovery = stage;
		handleRecovery(stage, target);
	}

	private void handleRecovery(ShadowIronCombatPolicy.RecoveryStage stage,
			LivingEntity target) {
		switch (stage) {
			case REPATH -> {
				iron.getNavigation().stop();
				nextPathTick = 0L;
				requestPath(target, APPROACH_SPEED, true);
			}
			case ESCAPE -> {
				iron.getJumpControl().jump();
				Vec3 escape = DefaultRandomPos.getPosAway(iron, 7, 4,
						target.position());
				if (escape != null && tryPathTo(escape, INTERCEPT_SPEED))
					return;
				requestPath(target, APPROACH_SPEED, true);
			}
			case RECALL -> {
				if (ShadowMonarchManager.tryRecoverStuckShadowNearOwner(iron,
						target))
					resetProgress(target);
				else {
					stalledTicks = ShadowIronCombatPolicy.REPATH_STALLED_TICKS;
					handledRecovery =
							ShadowIronCombatPolicy.RecoveryStage.REPATH;
				}
			}
			case NONE -> {
			}
		}
	}

	private boolean tryPathTo(Vec3 destination, double speed) {
		BlockPos pos = BlockPos.containing(destination);
		if (!iron.level().hasChunkAt(pos)
				|| !iron.level().getWorldBorder().isWithinBounds(pos))
			return false;
		Path path = iron.getNavigation().createPath(pos, 1);
		return path != null && path.canReach()
				&& iron.getNavigation().moveTo(path, speed);
	}

	private void resetProgress(LivingEntity target) {
		lastPosition = iron.position();
		lastTargetDistanceSqr = target == null ? Double.POSITIVE_INFINITY
				: iron.distanceToSqr(target);
		stalledTicks = 0;
		handledRecovery = ShadowIronCombatPolicy.RecoveryStage.NONE;
	}

	private boolean isSafeTarget(LivingEntity target) {
		return target != null && target.level() == iron.level()
				&& ShadowMonarchManager.canShadowDamage(iron, target);
	}

	private static double square(double value) {
		return value * value;
	}
}
