package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class ShamanMagicWhileProjectileFlyingTickProcedure {
	private static final double HOMING_SPEED = 0.25D;
	private static final double HOMING_TURN_WEIGHT = 0.35D;
	private static final int HOMING_TICKS = 40;
	private static final int MAX_LIFETIME_TICKS = 100;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity) {
		if (entity == null || immediatesourceentity == null)
			return;
		immediatesourceentity.setNoGravity(true);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.SHAMAN_MAGIC_PARTICLE.get()), (immediatesourceentity.getX()), (immediatesourceentity.getY()), (immediatesourceentity.getZ()), 2, 0.05, 0.05, 0.05, 0);
		immediatesourceentity.getPersistentData().putDouble("life", (immediatesourceentity.getPersistentData().getDouble("life") + 1));
		LivingEntity target = entity instanceof Mob mob ? mob.getTarget() : null;
		if (immediatesourceentity.getPersistentData().getDouble("life") <= HOMING_TICKS
				&& target != null && target.isAlive()) {
			Vec3 toTarget = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ())
					.subtract(immediatesourceentity.position());
			if (toTarget.lengthSqr() > 1.0E-6D) {
				Vec3 desiredVelocity = toTarget.normalize().scale(HOMING_SPEED);
				Vec3 currentVelocity = immediatesourceentity.getDeltaMovement();
				Vec3 steeredVelocity = currentVelocity.scale(1.0D - HOMING_TURN_WEIGHT)
						.add(desiredVelocity.scale(HOMING_TURN_WEIGHT));
				if (steeredVelocity.lengthSqr() > 1.0E-6D)
					immediatesourceentity.setDeltaMovement(steeredVelocity.normalize().scale(HOMING_SPEED));
			}
		}
		if (immediatesourceentity.getPersistentData().getDouble("life") >= MAX_LIFETIME_TICKS) {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
