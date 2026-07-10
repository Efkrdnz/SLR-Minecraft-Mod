package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/**
 * Baran calls down a lightning storm around the target.
 *
 * Timeline (MF ticks):
 *   1   – windup animation + ominous sound + yellow FLASH particles
 *   10  – first bolt cluster (3 bolts around target)
 *   20  – second bolt cluster (3 bolts, tighter)
 *   30  – third bolt cluster (4 bolts in a ring) — phase 2: 6 bolts
 *   40  – direct strike on target position (deals 20 damage separately)
 *   Phase 2 extra: MF=50 → fourth bolt cluster (5 bolts)
 *   ≥65 – reset to idle  (phase 2: ≥80)
 *
 * Baran himself is immune to lightning, so bolts are real (not visual-only).
 */
public class BaranLightningStormProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;
		if (!baran.getState().equals("lightning_storm"))
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
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 2, target.getZ(), 5, 0.5, 0.5, 0.5, 0);
				sl.playSound(null, BlockPos.containing(target.getX(), target.getY(), target.getZ()),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.lightning_bolt.thunder")),
						SoundSource.HOSTILE, 1.2f, 1.5f);
				// Warn players nearby
				for (net.minecraft.server.level.ServerPlayer sp : sl.players()) {
					if (sp.distanceTo(baran) <= 100) {
						sp.displayClientMessage(
								net.minecraft.network.chat.Component.literal("§e⚡ Baran calls down the storm!"), true);
					}
				}
			}
		}

		if (MF == 10) {
			spawnLightningCluster(world, baran, target, 3, 6.0);
		}

		if (MF == 20) {
			spawnLightningCluster(world, baran, target, 3, 4.0);
		}

		if (MF == 30) {
			int count = phase2 ? 6 : 4;
			spawnLightningRing(world, baran, target, count, 5.0);
		}

		if (MF == 40) {
			// Direct strike on target
			if (world instanceof ServerLevel sl) {
				spawnLightningAt(sl, target.getX(), target.getY(), target.getZ(), false);
				// Extra magic damage from the concentrated strike
				target.hurt(new net.minecraft.world.damagesource.DamageSource(
						world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
								.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT), baran), 20f);
			}
		}

		// Phase 2 extra salvo
		if (phase2 && MF == 50) {
			spawnLightningCluster(world, baran, target, 5, 7.0);
		}

		int resetAt = phase2 ? 80 : 65;
		if (MF >= resetAt) {
			resetToIdle(baran);
		}
	}

	/** Spawns {@code count} lightning bolts randomly scattered around the target. */
	private static void spawnLightningCluster(LevelAccessor world, BaranEntity baran,
			LivingEntity target, int count, double radius) {
		if (!(world instanceof ServerLevel sl)) return;
		double tx = target.getX(), ty = target.getY(), tz = target.getZ();
		for (int i = 0; i < count; i++) {
			double angle = Math.random() * Math.PI * 2;
			double dist = 1.0 + Math.random() * radius;
			double lx = tx + Math.cos(angle) * dist;
			double lz = tz + Math.sin(angle) * dist;
			spawnLightningAt(sl, lx, ty, lz, false);
		}
	}

	/** Spawns {@code count} lightning bolts evenly spaced in a ring around the target. */
	private static void spawnLightningRing(LevelAccessor world, BaranEntity baran,
			LivingEntity target, int count, double radius) {
		if (!(world instanceof ServerLevel sl)) return;
		double tx = target.getX(), ty = target.getY(), tz = target.getZ();
		for (int i = 0; i < count; i++) {
			double angle = (i / (double) count) * Math.PI * 2;
			double lx = tx + Math.cos(angle) * radius;
			double lz = tz + Math.sin(angle) * radius;
			spawnLightningAt(sl, lx, ty, lz, false);
		}
	}

	private static void spawnLightningAt(ServerLevel sl, double lx, double ly, double lz, boolean visualOnly) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
		if (bolt != null) {
			bolt.moveTo(lx, ly, lz);
			bolt.setVisualOnly(visualOnly);
			sl.addFreshEntity(bolt);
		}
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
