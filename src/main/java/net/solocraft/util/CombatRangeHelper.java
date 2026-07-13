package net.solocraft.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Range and movement helpers that account for the physical edges of entity hitboxes. */
public final class CombatRangeHelper {
	private static final String NEXT_RANGED_PATH_TICK = "slr_next_ranged_path_tick";

	private CombatRangeHelper() {
	}

	/** Returns the shortest three-dimensional gap between two entity hitboxes. */
	public static double surfaceDistance(Entity first, Entity second) {
		if (first == null || second == null)
			return Double.POSITIVE_INFINITY;
		AABB a = first.getBoundingBox();
		AABB b = second.getBoundingBox();
		double dx = axisGap(a.minX, a.maxX, b.minX, b.maxX);
		double dy = axisGap(a.minY, a.maxY, b.minY, b.maxY);
		double dz = axisGap(a.minZ, a.maxZ, b.minZ, b.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Returns the horizontal gap between hitboxes, ignoring height differences. */
	public static double horizontalSurfaceDistance(Entity first, Entity second) {
		if (first == null || second == null)
			return Double.POSITIVE_INFINITY;
		AABB a = first.getBoundingBox();
		AABB b = second.getBoundingBox();
		double dx = axisGap(a.minX, a.maxX, b.minX, b.maxX);
		double dz = axisGap(a.minZ, a.maxZ, b.minZ, b.maxZ);
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static boolean withinSurfaceRange(Entity first, Entity second, double range) {
		return surfaceDistance(first, second) <= range;
	}

	/**
	 * Keeps a ranged mob inside a useful firing band. Path requests are staggered by
	 * entity id to avoid recalculating every ranged mob on the same server tick.
	 */
	public static void maintainRangedBand(Entity entity, Entity target, double minimumRange,
			double maximumRange, double speed) {
		if (!(entity instanceof Mob mob) || !(target instanceof LivingEntity livingTarget)
				|| entity.level().isClientSide() || !livingTarget.isAlive())
			return;

		double distance = surfaceDistance(entity, target);
		long gameTime = entity.level().getGameTime();
		long nextPathTick = entity.getPersistentData().getLong(NEXT_RANGED_PATH_TICK);
		boolean recalculate = gameTime >= nextPathTick;
		if (recalculate)
			entity.getPersistentData().putLong(NEXT_RANGED_PATH_TICK,
					gameTime + 8L + Math.floorMod(entity.getId(), 4));
		mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (distance > maximumRange || !mob.getSensing().hasLineOfSight(livingTarget)) {
			if (recalculate)
				mob.getNavigation().moveTo(livingTarget, speed);
			return;
		}

		if (distance < minimumRange) {
			mob.getNavigation().stop();
			if (!recalculate)
				return;
			if (mob instanceof PathfinderMob pathfinder) {
				Vec3 retreat = DefaultRandomPos.getPosAway(pathfinder, 10, 5, target.position());
				if (retreat != null && retreat.distanceToSqr(target.position()) > entity.distanceToSqr(target)) {
					mob.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, speed * 1.08D);
					return;
				}
			}

			Vec3 away = entity.position().subtract(target.position());
			away = new Vec3(away.x, 0.0D, away.z);
			if (away.lengthSqr() > 1.0E-5D) {
				away = away.normalize().scale(0.16D * speed);
				entity.setDeltaMovement(entity.getDeltaMovement().add(away.x, 0.0D, away.z));
				entity.hasImpulse = true;
			}
			return;
		}

		mob.getNavigation().stop();
	}

	private static double axisGap(double firstMin, double firstMax, double secondMin, double secondMax) {
		if (firstMax < secondMin)
			return secondMin - firstMax;
		if (secondMax < firstMin)
			return firstMin - secondMax;
		return 0.0D;
	}
}
