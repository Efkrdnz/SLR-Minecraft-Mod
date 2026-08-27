package net.solocraft.entity.ai;

import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Selective airborne combat for summoned Beru.
 *
 * <p>Normal targets stay with the ground melee goal. True flying targets keep
 * this goal active, while bosses receive only occasional short aerial bursts.
 * Three-dimensional pursuit uses flying paths first and falls back to direct
 * steering, with a local escape/teleport watchdog for obstructed spaces.</p>
 */
public final class BeruShadowAerialCombatGoal extends Goal {
	private static final double HOVER_HEIGHT = 0.65D;
	private static final double CLAW_REACH_SQR = 11.0D;
	private static final double DASH_RANGE_SQR = 144.0D;
	private static final double TELEPORT_RANGE_SQR = 225.0D;
	private static final double SLAM_TRIGGER_RANGE_SQR = 196.0D;
	private static final double SLAM_RADIUS = 3.75D;
	private static final int CLAW_COOLDOWN_TICKS = 18;
	private static final int BOSS_FLIGHT_DURATION_MIN = 50;
	private static final int BOSS_FLIGHT_DURATION_VARIANCE = 31;
	private static final int BOSS_FLIGHT_COOLDOWN_MIN = 180;
	private static final int BOSS_FLIGHT_COOLDOWN_VARIANCE = 121;
	private static final int BOSS_INITIAL_DELAY_MIN = 80;
	private static final int BOSS_INITIAL_DELAY_VARIANCE = 61;
	private static final int STUCK_TICKS_BEFORE_ESCAPE = 14;
	private static final int ESCAPE_TICKS = 14;

	private final BeruShadowEntity beru;
	private int clawCooldown;
	private int dashTicks;
	private int teleportCooldown = 30;
	private int slamCooldown = 100;
	private int phaseTicks;
	private int bossFlightTicksRemaining;
	private int noProgressTicks;
	private int escapeTicks;
	private int escapeAttempts;
	private boolean bossBurst;
	private LivingEntity scheduledBoss;
	private long nextBossFlightTick = Long.MAX_VALUE;
	private FlightPhase phase = FlightPhase.CHASE;
	private Vec3 divePoint = Vec3.ZERO;
	private Vec3 escapePoint = Vec3.ZERO;
	private Vec3 lastProgressPosition = Vec3.ZERO;

	public BeruShadowAerialCombatGoal(BeruShadowEntity beru) {
		this.beru = beru;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = beru.getTarget();
		if (!isSafeTarget(target)) {
			clearBossSchedule();
			return false;
		}
		if (BeruShadowFlightPolicy.requiresSustainedFlight(target))
			return true;
		if (!BeruShadowFlightPolicy.isBossTarget(target)) {
			clearBossSchedule();
			return false;
		}

		long now = beru.level().getGameTime();
		if (scheduledBoss != target) {
			scheduledBoss = target;
			nextBossFlightTick = now + BOSS_INITIAL_DELAY_MIN
					+ beru.getRandom().nextInt(BOSS_INITIAL_DELAY_VARIANCE);
			return false;
		}
		return now >= nextBossFlightTick;
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = beru.getTarget();
		if (!isSafeTarget(target))
			return false;
		if (BeruShadowFlightPolicy.requiresSustainedFlight(target))
			return true;
		return bossBurst && target == scheduledBoss
				&& BeruShadowFlightPolicy.isBossTarget(target)
				&& bossFlightTicksRemaining > 0;
	}

	@Override
	public void start() {
		LivingEntity target = beru.getTarget();
		bossBurst = target != null
				&& !BeruShadowFlightPolicy.requiresSustainedFlight(target)
				&& BeruShadowFlightPolicy.isBossTarget(target);
		bossFlightTicksRemaining = bossBurst
				? BOSS_FLIGHT_DURATION_MIN
						+ beru.getRandom().nextInt(BOSS_FLIGHT_DURATION_VARIANCE)
				: 0;
		beru.setAerialCombatActive(true);
		beru.setFlightMode(true);
		beru.setAggressive(true);
		beru.getNavigation().stop();
		beru.setAnimation("start_flying");
		lastProgressPosition = beru.position();
		noProgressTicks = 0;
		escapeTicks = 0;
		escapeAttempts = 0;
	}

	@Override
	public void stop() {
		LivingEntity target = beru.getTarget();
		if (bossBurst && target == scheduledBoss && isSafeTarget(target))
			nextBossFlightTick = beru.level().getGameTime()
					+ BOSS_FLIGHT_COOLDOWN_MIN
					+ beru.getRandom().nextInt(BOSS_FLIGHT_COOLDOWN_VARIANCE);
		beru.setAerialCombatActive(false);
		beru.setAggressive(false);
		beru.getNavigation().stop();
		beru.setFlightMode(false);
		dashTicks = 0;
		phaseTicks = 0;
		bossFlightTicksRemaining = 0;
		bossBurst = false;
		noProgressTicks = 0;
		escapeTicks = 0;
		escapeAttempts = 0;
		phase = FlightPhase.CHASE;
	}

	@Override
	public void tick() {
		LivingEntity target = beru.getTarget();
		if (!isSafeTarget(target))
			return;

		clawCooldown = Math.max(0, clawCooldown - 1);
		teleportCooldown = Math.max(0, teleportCooldown - 1);
		slamCooldown = Math.max(0, slamCooldown - 1);
		if (bossBurst)
			bossFlightTicksRemaining--;
		beru.setFlightMode(true);
		beru.getLookControl().setLookAt(target, 60.0F, 60.0F);
		if (tickStuckRecovery(target))
			return;

		switch (phase) {
			case ASCEND -> tickAscent(target);
			case DIVE -> tickDive(target);
			case RECOVER -> tickRecovery(target);
			case CHASE -> tickChase(target);
		}
	}

	private void tickChase(LivingEntity target) {
		double distanceSqr = beru.distanceToSqr(target);
		if (slamCooldown <= 0 && distanceSqr <= SLAM_TRIGGER_RANGE_SQR
				&& beru.hasLineOfSight(target)) {
			beginDiveSlam(target);
			return;
		}

		if (teleportCooldown <= 0 && distanceSqr >= TELEPORT_RANGE_SQR
				&& tryTeleportBehind(target, true)) {
			teleportCooldown = 70 + beru.getRandom().nextInt(51);
			distanceSqr = beru.distanceToSqr(target);
		}

		if (dashTicks > 0) {
			tickAerialDash(target);
			return;
		}

		double targetCenterY = target.getY() + target.getBbHeight() * 0.62D;
		double hoverOffset = BeruShadowFlightPolicy.requiresSustainedFlight(target)
				? 0.15D : HOVER_HEIGHT;
		double hoverY = targetCenterY + hoverOffset
				+ Math.sin((beru.tickCount + beru.getId()) * 0.16D) * 0.25D;
		double speed = distanceSqr > 64.0D ? 2.4D : 1.8D;
		if (beru.getNavigation().isDone() || beru.tickCount % 8 == 0) {
			boolean foundPath = beru.getNavigation().moveTo(target.getX(), hoverY,
					target.getZ(), speed);
			if (!foundPath)
				beru.getMoveControl().setWantedPosition(target.getX(), hoverY,
						target.getZ(), speed);
		}

		if (clawCooldown > 0)
			return;
		if (distanceSqr <= CLAW_REACH_SQR) {
			performClawStrike(target);
		} else if (distanceSqr <= DASH_RANGE_SQR && beru.hasLineOfSight(target)) {
			dashTicks = 7;
			beru.setAnimation("flyattack");
		}
	}

	private void tickAerialDash(LivingEntity target) {
		dashTicks--;
		beru.getNavigation().stop();
		Vec3 aim = target.getEyePosition().subtract(beru.position());
		if (aim.lengthSqr() > 1.0E-4D) {
			Vec3 velocity = aim.normalize().scale(1.05D);
			setDirectVelocity(velocity);
		}
		if (beru.getBoundingBox().inflate(0.8D)
				.intersects(target.getBoundingBox())
				|| beru.distanceToSqr(target) <= CLAW_REACH_SQR) {
			performClawStrike(target);
			dashTicks = 0;
		} else if (dashTicks == 0) {
			clawCooldown = 7;
		}
	}

	private void performClawStrike(LivingEntity target) {
		if (!isSafeTarget(target))
			return;
		beru.setAnimation("flyattack");
		beru.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		if (beru.doHurtTarget(target)) {
			Vec3 knockback = target.position().subtract(beru.position());
			if (knockback.lengthSqr() > 1.0E-4D) {
				Vec3 impulse = knockback.normalize().scale(0.55D);
				target.push(impulse.x, 0.32D, impulse.z);
			}
			emitImpact(target.getX(), target.getY() + target.getBbHeight() * 0.5D,
					target.getZ(), false);
		}
		Vec3 rebound = beru.position().subtract(target.position());
		if (rebound.lengthSqr() > 1.0E-4D) {
			Vec3 impulse = rebound.normalize().scale(0.42D);
			setDirectVelocity(new Vec3(impulse.x, 0.45D, impulse.z));
		}
		clawCooldown = CLAW_COOLDOWN_TICKS;
	}

	private void beginDiveSlam(LivingEntity target) {
		phase = FlightPhase.ASCEND;
		phaseTicks = 0;
		divePoint = target.position();
		slamCooldown = 150 + beru.getRandom().nextInt(71);
		beru.setAnimation("start_flying");
		beru.getNavigation().stop();
	}

	private void tickAscent(LivingEntity target) {
		phaseTicks++;
		divePoint = target.position();
		double apexY = target.getY() + target.getBbHeight() + 6.0D;
		beru.getMoveControl().setWantedPosition(target.getX(), apexY,
				target.getZ(), 2.75D);
		if (phaseTicks >= 14 || beru.getY() >= apexY - 1.0D) {
			phase = FlightPhase.DIVE;
			phaseTicks = 0;
			divePoint = target.position().add(0.0D, 0.15D, 0.0D);
			beru.setAnimation("special_attack");
		}
	}

	private void tickDive(LivingEntity target) {
		phaseTicks++;
		// Keep a little target tracking at the start, then commit to the impact
		// point so the slam remains readable and can be dodged.
		if (phaseTicks <= 4)
			divePoint = target.position().add(0.0D, 0.15D, 0.0D);
		Vec3 diveVector = divePoint.subtract(beru.position());
		if (diveVector.lengthSqr() > 1.0E-4D) {
			Vec3 velocity = diveVector.normalize().scale(1.45D);
			setDirectVelocity(new Vec3(velocity.x,
					Math.min(-0.85D, velocity.y), velocity.z));
		}
		boolean reachedImpact = beru.position().distanceToSqr(divePoint) <= 5.0D
				|| beru.getY() <= divePoint.y + 0.65D;
		if (reachedImpact || phaseTicks >= 14)
			performDiveSlam();
	}

	private void performDiveSlam() {
		beru.setAnimation("special_attack");
		beru.setDeltaMovement(Vec3.ZERO);
		double damage = Math.max(6.0D,
				beru.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.72D);
		AABB impactArea = beru.getBoundingBox().inflate(SLAM_RADIUS, 2.25D,
				SLAM_RADIUS);
		for (LivingEntity candidate : beru.level().getEntitiesOfClass(
				LivingEntity.class, impactArea,
				target -> ShadowMonarchManager.canShadowDamage(beru, target))) {
			if (candidate.hurt(beru.level().damageSources().mobAttack(beru),
					(float) damage)) {
				Vec3 away = candidate.position().subtract(beru.position());
				if (away.lengthSqr() > 1.0E-4D) {
					Vec3 impulse = away.normalize().scale(0.75D);
					candidate.push(impulse.x, 0.65D, impulse.z);
				}
			}
		}
		emitImpact(beru.getX(), beru.getY() + 0.25D, beru.getZ(), true);
		phase = FlightPhase.RECOVER;
		phaseTicks = 0;
		clawCooldown = 16;
	}

	private void tickRecovery(LivingEntity target) {
		phaseTicks++;
		beru.getMoveControl().setWantedPosition(beru.getX(),
				Math.max(beru.getY() + 0.2D, target.getEyeY() + HOVER_HEIGHT),
				beru.getZ(), 1.15D);
		if (phaseTicks >= 9) {
			phase = FlightPhase.CHASE;
			phaseTicks = 0;
		}
	}

	private boolean tickStuckRecovery(LivingEntity target) {
		if (escapeTicks > 0)
			return tickEscape();
		if (phase != FlightPhase.CHASE || dashTicks > 0) {
			lastProgressPosition = beru.position();
			noProgressTicks = 0;
			return false;
		}

		double movedSqr = beru.position().distanceToSqr(lastProgressPosition);
		lastProgressPosition = beru.position();
		boolean controllerBlocked = beru.getMoveControl()
				instanceof BeruFlightMoveControl control
				&& control.isBlockedFor(5);
		boolean shouldBeClosing = beru.distanceToSqr(target)
				> CLAW_REACH_SQR * 1.35D;
		boolean stalled = movedSqr < 0.0025D
				|| (beru.horizontalCollision || controllerBlocked)
						&& movedSqr < 0.025D;
		if (shouldBeClosing && stalled)
			noProgressTicks++;
		else {
			noProgressTicks = Math.max(0, noProgressTicks - 2);
			if (movedSqr > 0.04D)
				escapeAttempts = 0;
		}
		if (noProgressTicks < STUCK_TICKS_BEFORE_ESCAPE)
			return false;

		noProgressTicks = 0;
		escapeAttempts++;
		if (escapeAttempts >= 3) {
			if (bossBurst) {
				bossFlightTicksRemaining = 0;
				return false;
			}
			if (tryTeleportBehind(target, false)) {
				escapeAttempts = 0;
				return true;
			}
		}

		Vec3 destination = findEscapePoint(target);
		if (destination == null) {
			if (bossBurst)
				bossFlightTicksRemaining = 0;
			else if (tryTeleportBehind(target, false))
				escapeAttempts = 0;
			return false;
		}

		escapePoint = destination;
		escapeTicks = ESCAPE_TICKS;
		beru.getNavigation().stop();
		if (beru.getMoveControl() instanceof BeruFlightMoveControl control)
			control.resetBlockedState();
		return tickEscape();
	}

	private boolean tickEscape() {
		if (beru.position().distanceToSqr(escapePoint) < 0.45D
				|| --escapeTicks <= 0) {
			escapeTicks = 0;
			lastProgressPosition = beru.position();
			if (beru.getMoveControl() instanceof BeruFlightMoveControl control)
				control.resetBlockedState();
			return false;
		}
		beru.getNavigation().stop();
		beru.getMoveControl().setWantedPosition(escapePoint.x, escapePoint.y,
				escapePoint.z, 2.25D);
		return true;
	}

	private Vec3 findEscapePoint(LivingEntity target) {
		Vec3 towardTarget = target.getEyePosition().subtract(beru.position());
		Vec3 forward = new Vec3(towardTarget.x, 0.0D, towardTarget.z);
		if (forward.lengthSqr() < 1.0E-4D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		else
			forward = forward.normalize();
		Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
		if ((beru.getId() & 1) != 0)
			side = side.scale(-1.0D);
		double vertical = Mth.clamp(towardTarget.y, -1.8D, 2.4D);
		Vec3[] offsets = {
				side.scale(3.0D).add(forward.scale(0.8D)).add(0.0D, 0.8D, 0.0D),
				side.scale(-3.0D).add(forward.scale(0.8D)).add(0.0D, 1.1D, 0.0D),
				forward.scale(2.5D).add(0.0D, Math.max(1.0D, vertical), 0.0D),
				new Vec3(0.0D, 2.6D, 0.0D),
				forward.scale(1.4D).add(0.0D, Math.min(-0.8D, vertical), 0.0D),
				forward.scale(-2.0D).add(0.0D, 1.4D, 0.0D)
		};
		for (Vec3 offset : offsets) {
			Vec3 destination = beru.position().add(offset);
			if (canFlyDirectlyTo(destination))
				return destination;
		}
		return null;
	}

	private boolean canFlyDirectlyTo(Vec3 destination) {
		BlockPos blockPos = BlockPos.containing(destination);
		if (!beru.level().hasChunkAt(blockPos)
				|| !beru.level().getWorldBorder().isWithinBounds(blockPos))
			return false;
		Vec3 offset = destination.subtract(beru.position());
		int samples = Math.max(1, Mth.ceil(offset.length() / 0.35D));
		for (int sample = 1; sample <= samples; sample++) {
			AABB bounds = beru.getBoundingBox().move(
					offset.scale((double) sample / samples));
			if (!beru.level().noCollision(beru, bounds))
				return false;
		}
		return true;
	}

	/**
	 * Collision-safe version of boss Beru's long-range teleport. It tests
	 * several offsets behind/alongside the victim and simply keeps flying if
	 * every candidate is obstructed.
	 */
	private boolean tryTeleportBehind(LivingEntity target, boolean attackCue) {
		Vec3 backwards = target.getLookAngle().multiply(-1.0D, 0.0D, -1.0D);
		if (backwards.lengthSqr() < 1.0E-4D)
			backwards = target.position().subtract(beru.position())
					.multiply(-1.0D, 0.0D, -1.0D);
		if (backwards.lengthSqr() < 1.0E-4D)
			backwards = new Vec3(0.0D, 0.0D, 1.0D);
		backwards = backwards.normalize();
		Vec3 sideways = new Vec3(-backwards.z, 0.0D, backwards.x);
		double[] sideOffsets = {0.0D, 1.8D, -1.8D};
		double[] heightOffsets = {1.2D, 2.8D, 0.2D};
		for (double height : heightOffsets) {
			for (double side : sideOffsets) {
				Vec3 destination = target.position()
						.add(backwards.scale(2.4D))
						.add(sideways.scale(side))
						.add(0.0D, height, 0.0D);
				if (canTeleportTo(destination)) {
					beru.teleportTo(destination.x, destination.y, destination.z);
					beru.setDeltaMovement(Vec3.ZERO);
					if (attackCue) {
						beru.setAnimation("flyattack");
						emitImpact(destination.x, destination.y + 0.8D,
								destination.z, false);
					}
					lastProgressPosition = beru.position();
					noProgressTicks = 0;
					return true;
				}
			}
		}
		return false;
	}

	private boolean canTeleportTo(Vec3 destination) {
		BlockPos blockPos = BlockPos.containing(destination);
		if (!beru.level().hasChunkAt(blockPos))
			return false;
		WorldBorder border = beru.level().getWorldBorder();
		if (!border.isWithinBounds(blockPos))
			return false;
		Vec3 offset = destination.subtract(beru.position());
		AABB movedBounds = beru.getBoundingBox().move(offset);
		return beru.level().noCollision(beru, movedBounds);
	}

	private boolean isSafeTarget(LivingEntity target) {
		return target != null && target.level() == beru.level()
				&& ShadowMonarchManager.canShadowDamage(beru, target);
	}

	private void clearBossSchedule() {
		scheduledBoss = null;
		nextBossFlightTick = Long.MAX_VALUE;
		bossFlightTicksRemaining = 0;
		bossBurst = false;
	}

	private void setDirectVelocity(Vec3 velocity) {
		if (beru.getMoveControl() instanceof BeruFlightMoveControl control)
			control.setDirectVelocity(velocity);
		else {
			beru.setDeltaMovement(velocity);
			beru.hasImpulse = true;
		}
	}

	private void emitImpact(double x, double y, double z, boolean slam) {
		if (!(beru.level() instanceof ServerLevel level))
			return;
		level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.IMPACT_22.get(),
				x, y, z, slam ? 8 : 3, slam ? 0.9D : 0.18D,
				slam ? 0.25D : 0.45D, slam ? 0.9D : 0.18D,
				slam ? 0.08D : 0.0D);
		if (slam)
			level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 4,
					0.75D, 0.15D, 0.75D, 0.0D);
		level.playSound(null, BlockPos.containing(x, y, z),
				BuiltInRegistries.SOUND_EVENT.get(
						ResourceLocation.parse("entity.generic.explode")),
				SoundSource.NEUTRAL, slam ? 1.45F : 0.8F,
				slam ? 0.85F : Mth.nextFloat(beru.getRandom(), 1.1F, 1.35F));
	}

	private enum FlightPhase {
		CHASE,
		ASCEND,
		DIVE,
		RECOVER
	}
}
