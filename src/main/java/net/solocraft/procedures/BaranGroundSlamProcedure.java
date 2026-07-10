package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Baran slams the ground with his sword sending a devastating shockwave.
 *
 * Timeline (MF ticks):
 *   1   – raise weapon animation, crackling ground particles
 *   12  – SLAM! First shockwave (radius 7): heavy damage + knockback + EXPLOSION particles
 *   22  – Second shockwave (radius 12): moderate damage + SMOKE
 *   Phase 2 – a third shockwave at MF=32 (radius 16, lower damage)
 *   ≥50 – reset to idle  (phase 2: ≥65)
 */
public class BaranGroundSlamProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;
		if (!baran.getState().equals("ground_slam"))
			return;

		LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		if (target == null) {
			resetToIdle(baran);
			return;
		}

		double MF = baran.getPersistentData().getDouble("MF");
		boolean phase2 = baran.getPersistentData().getBoolean("baran_phase2");

		if (MF == 1) {
			baran.animationprocedure = "attack";
			// Ground crack warning
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.CRIT, x, y + 0.1, z, 20, 2.0, 0.1, 2.0, 0.1);
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.ravager.roar")),
						SoundSource.HOSTILE, 1.5f, 0.8f);
			}
		}

		if (MF == 12) {
			// Primary slam
			dealShockwave(world, baran, x, y, z, 7.0,
					phase2 ? 35f : 28f, true, ParticleTypes.EXPLOSION);
			if (world instanceof ServerLevel sl) {
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")),
						SoundSource.HOSTILE, 2.0f, 0.5f);
			}
		}

		if (MF == 22) {
			// Secondary ripple
			dealShockwave(world, baran, x, y, z, 12.0,
					phase2 ? 18f : 14f, false, ParticleTypes.LARGE_SMOKE);
			if (world instanceof ServerLevel sl) {
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.stone.break")),
						SoundSource.HOSTILE, 2.0f, 0.4f);
			}
		}

		// Phase 2 third wave
		if (phase2 && MF == 32) {
			dealShockwave(world, baran, x, y, z, 16.0, 10f, false, ParticleTypes.CAMPFIRE_COSY_SMOKE);
		}

		int resetAt = phase2 ? 65 : 50;
		if (MF >= resetAt) {
			resetToIdle(baran);
		}
	}

	/**
	 * Damages all living entities within {@code radius} blocks of Baran.
	 * @param knockback whether to apply vertical knockback (true = first slam only)
	 */
	private static void dealShockwave(LevelAccessor world, BaranEntity baran,
			double x, double y, double z, double radius, float damage,
			boolean knockback, net.minecraft.core.particles.SimpleParticleType particle) {
		if (!(world instanceof ServerLevel sl)) return;

		DamageSource src = new DamageSource(
				world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(DamageTypes.MOB_ATTACK), baran);

		for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
				baran.getBoundingBox().inflate(radius + 2),
				e -> e != baran && !isWildKaiselin(e) && e.distanceTo(baran) <= radius)) {

			nearby.hurt(src, damage);

			if (knockback) {
				// Push entity away from Baran
				Vec3 dir = nearby.position().subtract(baran.position()).normalize();
				nearby.setDeltaMovement(dir.x * 1.2, 0.6, dir.z * 1.2);
				nearby.hurtMarked = true;
			}

			sl.sendParticles(particle,
					nearby.getX(), nearby.getY() + 0.5, nearby.getZ(),
					5, 0.3, 0.3, 0.3, 0.0);
		}

		// Ring of particles at slam location
		for (int i = 0; i < 16; i++) {
			double angle = (i / 16.0) * Math.PI * 2;
			double px = x + Math.cos(angle) * (radius * 0.8);
			double pz = z + Math.sin(angle) * (radius * 0.8);
			sl.sendParticles(particle, px, y + 0.2, pz, 3, 0.2, 0.1, 0.2, 0.0);
		}
	}

	private static boolean isWildKaiselin(LivingEntity entity) {
		return entity instanceof KaiselinEntity && entity.getType() == SololevelingModEntities.KAISELIN.get();
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
