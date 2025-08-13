package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.Comparator;

public class MagicMissileWhileProjectileFlyingTickProcedure {
	private static final double BASE_SPEED = 0.6; // Base movement speed
	private static final double HOMING_STRENGTH = 0.3; // Higher values make it turn faster
	private static final double MAX_LIFETIME = 80;
	private static final int HOMING_DELAY = 10; // Start homing earlier

	public static void execute(LevelAccessor world, double x, double y, double z, Entity projectile) {
		if (projectile == null)
			return;
		// Spawn fire particles
		if (world instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MAGIC_MISSILES.get()), x, y, z, 10, 0, 0, 0, 0);
		}
		// No gravity
		projectile.setNoGravity(true);
		// Tick life counter
		projectile.getPersistentData().putDouble("life", projectile.getPersistentData().getDouble("life") + 1);
		double life = projectile.getPersistentData().getDouble("life");
		if (life >= HOMING_DELAY) { // Enable homing after delay
			Vec3 projectilePos = projectile.position();
			// Get shooter and/or owner to exclude them from targeting
			Entity shooter = null;
			Entity owner = null;
			if (projectile instanceof Projectile proj) {
				shooter = proj.getOwner();
			}
			if (shooter instanceof OwnableEntity ownable) {
				owner = ownable.getOwner();
			}
			// Find target with priority on owner's target
			Entity target = findTargetWithPriority(world, projectilePos, 60, shooter, owner);
			if (target != null) {
				Vec3 targetPos = target.position().add(0, 1, 0); // Aim slightly above the entity
				Vec3 direction = targetPos.subtract(projectilePos).normalize(); // Direction vector to target
				// Get current velocity and blend it with target direction for smoother turning
				Vec3 currentVelocity = projectile.getDeltaMovement();
				Vec3 newVelocity = currentVelocity.lerp(direction.scale(BASE_SPEED), HOMING_STRENGTH);
				projectile.setDeltaMovement(newVelocity);
			}
		}
		// Auto-remove projectile after max lifetime
		if (life >= MAX_LIFETIME) {
			if (!projectile.level().isClientSide())
				projectile.discard();
		}
	}

	private static Entity findTargetWithPriority(LevelAccessor world, Vec3 pos, double range, Entity shooter, Entity owner) {
		if (!(world instanceof ServerLevel serverLevel)) {
			return null;
		}
		// PRIORITY 1: Check if the owner has a target (if owner is a Mob)
		if (owner instanceof Mob ownerMob && ownerMob.getTarget() != null) {
			Entity ownerTarget = ownerMob.getTarget();
			if (ownerTarget.isAlive() && ownerTarget.distanceToSqr(pos) <= range * range) {
				return ownerTarget;
			}
		}
		// PRIORITY 2: Check if the shooter has a target (if shooter is a Mob and different from owner)
		if (shooter instanceof Mob shooterMob && shooter != owner && shooterMob.getTarget() != null) {
			Entity shooterTarget = shooterMob.getTarget();
			if (shooterTarget.isAlive() && shooterTarget.distanceToSqr(pos) <= range * range) {
				return shooterTarget;
			}
		}
		// PRIORITY 3: Default to nearest valid entity
		return serverLevel.getEntitiesOfClass(Entity.class, new AABB(pos, pos).inflate(range), entity -> {
			// Exclude the projectile itself
			if (entity instanceof Projectile) {
				return false;
			}
			// Exclude the shooter
			if (shooter != null && entity.equals(shooter)) {
				return false;
			}
			// Exclude the owner
			if (owner != null && entity.equals(owner)) {
				return false;
			}
			//Exclude item entities
			if ((entity instanceof ItemEntity)) {
				return false;
			}
			if ((entity instanceof ExperienceOrb)) {
				return false;
			}
			// Can target any living entity
			return entity.isAlive();
		}).stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).orElse(null);
	}
}
