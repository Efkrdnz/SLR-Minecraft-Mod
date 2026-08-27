package net.solocraft.entity.ai;

import net.solocraft.entity.BeruShadowEntity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Responsive flight control for Shadow Beru. Vanilla FlyingMoveControl eases
 * toward its destination very slowly and keeps residual velocity after a path
 * ends, which made Beru look like it was gliding on ice. This controller uses
 * positive acceleration while commanded, collision-safe steering, and active
 * braking when the destination is reached.
 */
public final class BeruFlightMoveControl extends MoveControl {
	private static final double ARRIVAL_DISTANCE_SQR = 0.20D;
	private static final double MAX_FLIGHT_SPEED = 1.15D;

	private final BeruShadowEntity beru;
	private boolean directVelocityThisTick;
	private int blockedTicks;

	public BeruFlightMoveControl(BeruShadowEntity beru) {
		super(beru);
		this.beru = beru;
	}

	@Override
	public void tick() {
		if (directVelocityThisTick) {
			directVelocityThisTick = false;
			Vec3 velocity = beru.getDeltaMovement();
			orientToVelocity(velocity);
			beru.setSpeed((float) velocity.length());
			return;
		}
		if (operation != Operation.MOVE_TO) {
			brake();
			return;
		}
		operation = Operation.WAIT;
		Vec3 offset = new Vec3(wantedX - beru.getX(),
				wantedY - beru.getY(), wantedZ - beru.getZ());
		double distanceSqr = offset.lengthSqr();
		if (distanceSqr < ARRIVAL_DISTANCE_SQR) {
			blockedTicks = 0;
			brake();
			return;
		}

		double attributeSpeed = beru.getAttributeValue(Attributes.FLYING_SPEED);
		double requestedSpeed = Mth.clamp(speedModifier * attributeSpeed,
				0.22D, MAX_FLIGHT_SPEED);
		double distance = Math.sqrt(distanceSqr);
		Vec3 desired = offset.scale(1.0D / distance)
				.scale(Math.min(requestedSpeed, distance));
		double acceleration = beru.isAerialCombatActive() ? 0.48D : 0.36D;
		Vec3 next = beru.getDeltaMovement().scale(1.0D - acceleration)
				.add(desired.scale(acceleration));
		next = collisionSafeVelocity(next, desired);
		recordBlockedState(next);
		beru.setDeltaMovement(next);
		beru.setSpeed((float) requestedSpeed);
		beru.hasImpulse = true;
		orientToVelocity(next);
	}

	public void setDirectVelocity(Vec3 velocity) {
		directVelocityThisTick = true;
		operation = Operation.WAIT;
		Vec3 safeVelocity = collisionSafeVelocity(velocity, velocity);
		recordBlockedState(safeVelocity);
		beru.setDeltaMovement(safeVelocity);
		beru.hasImpulse = true;
	}

	public boolean isBlockedFor(int ticks) {
		return blockedTicks >= ticks;
	}

	public void resetBlockedState() {
		blockedTicks = 0;
	}

	private void orientToVelocity(Vec3 velocity) {
		double horizontal = Math.sqrt(
				velocity.x * velocity.x + velocity.z * velocity.z);
		if (horizontal > 1.0E-4D) {
			float desiredYaw = (float) (Mth.atan2(velocity.z, velocity.x)
					* Mth.RAD_TO_DEG) - 90.0F;
			beru.setYRot(rotlerp(beru.getYRot(), desiredYaw, 22.0F));
			beru.yBodyRot = beru.getYRot();
			float desiredPitch = (float) -(Mth.atan2(velocity.y, horizontal)
					* Mth.RAD_TO_DEG);
			beru.setXRot(Mth.lerp(0.24F, beru.getXRot(),
					Mth.clamp(desiredPitch, -45.0F, 45.0F)));
		}
	}

	private Vec3 collisionSafeVelocity(Vec3 proposed, Vec3 desired) {
		if (canMove(proposed))
			return proposed;

		double speed = Math.max(0.28D,
				Math.min(MAX_FLIGHT_SPEED, proposed.length()));
		Vec3 forward = new Vec3(desired.x, 0.0D, desired.z);
		if (forward.lengthSqr() < 1.0E-5D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		else
			forward = forward.normalize();
		Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
		if ((beru.getId() & 1) != 0)
			side = side.scale(-1.0D);
		double vertical = Mth.clamp(desired.y, -0.42D, 0.42D);
		Vec3[] alternatives = {
				forward.scale(speed * 0.72D).add(0.0D, vertical, 0.0D),
				side.scale(speed * 0.82D).add(forward.scale(speed * 0.24D))
						.add(0.0D, 0.18D, 0.0D),
				side.scale(-speed * 0.82D).add(forward.scale(speed * 0.24D))
						.add(0.0D, 0.24D, 0.0D),
				forward.scale(speed * 0.45D).add(0.0D, 0.46D, 0.0D),
				forward.scale(speed * 0.45D).add(0.0D, -0.36D, 0.0D),
				forward.scale(-speed * 0.55D).add(0.0D, 0.28D, 0.0D)
		};
		for (Vec3 alternative : alternatives) {
			if (canMove(alternative))
				return alternative;
		}
		return Vec3.ZERO;
	}

	private boolean canMove(Vec3 velocity) {
		if (velocity.lengthSqr() < 1.0E-6D)
			return true;
		int samples = Math.max(1, Mth.ceil(velocity.length() / 0.30D));
		for (int sample = 1; sample <= samples; sample++) {
			AABB moved = beru.getBoundingBox().move(
					velocity.scale((double) sample / samples));
			if (!beru.level().noCollision(beru, moved))
				return false;
		}
		return true;
	}

	private void recordBlockedState(Vec3 velocity) {
		if (velocity.lengthSqr() < 1.0E-5D)
			blockedTicks++;
		else
			blockedTicks = Math.max(0, blockedTicks - 2);
	}

	private void brake() {
		directVelocityThisTick = false;
		Vec3 movement = beru.getDeltaMovement();
		if (beru.isNoGravity()) {
			movement = movement.scale(0.52D);
			if (movement.lengthSqr() < 0.0025D)
				movement = Vec3.ZERO;
		} else {
			movement = new Vec3(movement.x * 0.58D, movement.y,
					movement.z * 0.58D);
		}
		beru.setDeltaMovement(movement);
		beru.setSpeed(0.0F);
	}
}
