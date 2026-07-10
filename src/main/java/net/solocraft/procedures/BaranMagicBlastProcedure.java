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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;

/**
 * Baran fires a powerful magic blast at the target.
 *
 * Timeline (MF ticks):
 *   1   – play attack animation, begin windup
 *   15  – spawn WITCH particle burst at Baran's position (charging energy)
 *   25  – primary magic impact at target: 30 damage + DRAGON_BREATH particles
 *          + explosion sound
 *   35  – secondary shockwave: 15 damage to all in radius 5 around target
 *          + SMOKE particles
 *   Phase 2 extra: at MF=20 also fires a smaller pre-shot (15 damage)
 *   ≥55 – reset to idle
 */
public class BaranMagicBlastProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;
		if (!baran.getState().equals("magic_blast"))
			return;

		LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		if (target == null) {
			resetToIdle(baran);
			return;
		}

		double MF = baran.getPersistentData().getDouble("MF");
		boolean phase2 = baran.getPersistentData().getBoolean("baran_phase2");

		if (MF == 1) {
			// Windup animation
			baran.animationprocedure = "attack";
			// Warning particles at Baran
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.ENCHANT, x, y + 1.5, z, 30, 1.0, 1.0, 1.0, 0.3);
			}
		}

		if (MF == 15) {
			// Charging energy visuals
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.WITCH, x, y + 1.5, z, 40, 0.5, 0.5, 0.5, 0.1);
				sl.sendParticles(ParticleTypes.PORTAL, x, y + 1.5, z, 20, 0.8, 0.8, 0.8, 0.2);
			}
		}

		// Phase 2 pre-shot
		if (phase2 && MF == 20) {
			double tx = target.getX(), ty = target.getY(), tz = target.getZ();
			target.hurt(magicDamage(world, baran), 15f);
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.DRAGON_BREATH, tx, ty + 0.5, tz, 15, 0.3, 0.3, 0.3, 0.05);
			}
		}

		if (MF == 25) {
			// Primary impact
			double tx = target.getX(), ty = target.getY(), tz = target.getZ();
			target.hurt(magicDamage(world, baran), phase2 ? 40f : 30f);
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.DRAGON_BREATH, tx, ty + 1, tz, 50, 0.8, 0.8, 0.8, 0.1);
				sl.sendParticles(ParticleTypes.FLASH, tx, ty + 1, tz, 3, 0.2, 0.2, 0.2, 0);
				sl.playSound(null, BlockPos.containing(tx, ty, tz),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")),
						SoundSource.HOSTILE, 1.5f, 0.6f);
			}
		}

		if (MF == 35) {
			// Shockwave around impact point
			double tx = target.getX(), ty = target.getY(), tz = target.getZ();
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.LARGE_SMOKE, tx, ty + 0.5, tz, 30, 2.5, 0.5, 2.5, 0.0);
				// Damage all in radius 5
				for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
						baran.getBoundingBox().inflate(300),
						e -> e != baran && !isWildKaiselin(e) && e.distanceTo(target) <= 5)) {
					nearby.hurt(magicDamage(world, baran), 15f);
				}
				sl.playSound(null, BlockPos.containing(tx, ty, tz),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.shoot")),
						SoundSource.HOSTILE, 1.5f, 1.2f);
			}
		}

		if (MF >= 55) {
			resetToIdle(baran);
		}
	}

	private static DamageSource magicDamage(LevelAccessor world, BaranEntity baran) {
		return new DamageSource(
				world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(DamageTypes.MAGIC), baran);
	}

	private static boolean isWildKaiselin(LivingEntity entity) {
		return entity instanceof KaiselinEntity && entity.getType() == SololevelingModEntities.KAISELIN.get();
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
