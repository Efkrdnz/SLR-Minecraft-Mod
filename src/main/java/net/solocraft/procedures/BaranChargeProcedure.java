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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * Baran charges toward the target at terrifying speed.
 *
 * Timeline (MF ticks):
 *   1   – apply SPEED II, trail particles, menacing sound
 *   5   – start propelling Baran toward target every tick
 *   5-18– propulsion ticks: deal 20 damage to any entity he collides with
 *   18  – deal guaranteed 25-damage hit if target is still within 6 blocks
 *          + knockback
 *   Phase 2: MF=1 gives SPEED III; collision damage = 28
 *   ≥40 – remove speed effect, reset to idle
 */
public class BaranChargeProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;
		if (!baran.getState().equals("charge"))
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
			// Speed burst
			int speedLevel = phase2 ? 2 : 1; // Speed II or III (0-indexed)
			baran.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedLevel, false, false));

			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.CRIT, x, y + 1, z, 15, 0.3, 0.3, 0.3, 0.3);
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.ravager.step")),
						SoundSource.HOSTILE, 1.5f, 0.6f);
			}
		}

		// Propulsion ticks: push Baran toward target
		if (MF >= 5 && MF <= 18) {
			Vec3 dir = target.position().subtract(baran.position()).normalize();
			double speed = phase2 ? 1.6 : 1.2;
			baran.setDeltaMovement(dir.x * speed, baran.getDeltaMovement().y, dir.z * speed);

			// Leave a trail of dark particles
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.5, z, 3, 0.2, 0.2, 0.2, 0.02);
				sl.sendParticles(ParticleTypes.CRIT, x, y + 0.5, z, 2, 0.2, 0.2, 0.2, 0.1);
			}

			// Collision damage: hurt anything close to Baran during the charge
			if (world instanceof ServerLevel sl) {
				float collisionDmg = phase2 ? 28f : 20f;
				DamageSource src = new DamageSource(
						world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
								.getHolderOrThrow(DamageTypes.MOB_ATTACK), baran);
				for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class,
						baran.getBoundingBox().inflate(1.5),
						e -> e != baran && !isWildKaiselin(e))) {
					// Only damage each entity once per charge
					String hitKey = "baran_charge_hit_" + nearby.getUUID();
					if (!baran.getPersistentData().getBoolean(hitKey)) {
						nearby.hurt(src, collisionDmg);
						baran.getPersistentData().putBoolean(hitKey, true);
					}
				}
			}
		}

		if (MF == 18) {
			// Final slam if close enough
			if (net.solocraft.util.CombatRangeHelper.withinSurfaceRange(baran, target, 6.0D)) {
				DamageSource src = new DamageSource(
						world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
								.getHolderOrThrow(DamageTypes.MOB_ATTACK), baran);
				target.hurt(src, phase2 ? 35f : 25f);
				// Knockback away
				Vec3 dir = target.position().subtract(baran.position()).normalize();
				target.setDeltaMovement(dir.x * 1.5, 0.5, dir.z * 1.5);
				target.hurtMarked = true;
			}
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, 3, 0.3, 0.3, 0.3, 0);
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")),
						SoundSource.HOSTILE, 1.0f, 1.2f);
			}
			// Clear per-entity hit flags
			clearChargeHitFlags(baran);
		}

		if (MF >= 40) {
			baran.removeEffect(MobEffects.MOVEMENT_SPEED);
			clearChargeHitFlags(baran);
			resetToIdle(baran);
		}
	}

	/** Removes per-entity collision flags stored during the charge. */
	private static void clearChargeHitFlags(BaranEntity baran) {
		java.util.Set<String> toRemove = new java.util.HashSet<>();
		for (String key : baran.getPersistentData().getAllKeys()) {
			if (key.startsWith("baran_charge_hit_")) toRemove.add(key);
		}
		toRemove.forEach(baran.getPersistentData()::remove);
	}

	private static boolean isWildKaiselin(LivingEntity entity) {
		return entity instanceof KaiselinEntity && entity.getType() == SololevelingModEntities.KAISELIN.get();
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
