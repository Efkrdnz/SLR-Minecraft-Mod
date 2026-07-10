package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;

/**
 * Baran tears open a dark rift and summons Demon Knights to fight for him.
 *
 * Timeline (MF ticks):
 *   1   – dark rift particles + summoning sound + warning message
 *   20  – spawn 2 Demon Knights (phase 2: 3 Knights) near Baran
 *   35  – spawn 1 more Demon Knight behind the target (ambush!) [phase 2 only]
 *   ≥55 – reset to idle
 *
 * Summoned knights have the dkc_floor_number and dkc_spawned_by tags
 * copied from Baran so they get counted by the kill counter correctly.
 */
public class BaranSummonProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;
		if (!baran.getState().equals("summon"))
			return;

		LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		if (target == null) {
			resetToIdle(baran);
			return;
		}

		double MF = baran.getPersistentData().getDouble("MF");
		boolean phase2 = baran.getPersistentData().getBoolean("baran_phase2");

		// Copy DKC tags from Baran so summoned knights count as floor kills
		int baranFloor = (int) baran.getPersistentData().getDouble("dkc_floor_number");
		String baranOwner = baran.getPersistentData().getString("dkc_spawned_by");

		if (MF == 1) {
			baran.animationprocedure = "attack";
			// Dark rift visuals at Baran's position
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 60, 1.5, 1.5, 1.5, 0.5);
				sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z, 20, 0.5, 0.5, 0.5, 0.02);
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.spawn")),
						SoundSource.HOSTILE, 1.0f, 1.5f);
				// Warn nearby players
				for (net.minecraft.server.level.ServerPlayer sp : sl.players()) {
					if (sp.distanceTo(baran) <= 100) {
						sp.displayClientMessage(
								net.minecraft.network.chat.Component.literal("§8☠ Baran summons his demon knights!"), true);
					}
				}
			}
		}

		if (MF == 20) {
			if (world instanceof ServerLevel sl) {
				int count = phase2 ? 3 : 2;
				for (int i = 0; i < count; i++) {
					double angle = (i / (double) count) * Math.PI * 2;
					double sx = x + Math.cos(angle) * 3;
					double sz = z + Math.sin(angle) * 3;
					spawnKnight(sl, baran, target, sx, y, sz, baranFloor, baranOwner);
				}
				sl.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 30, 2.0, 1.0, 2.0, 0.3);
			}
		}

		// Phase 2: surprise ambush knight behind the target
		if (phase2 && MF == 35) {
			if (world instanceof ServerLevel sl) {
				double tx = target.getX(), ty = target.getY(), tz = target.getZ();
				// Spawn behind the target (opposite of their look direction)
				double backX = tx - target.getLookAngle().x * 2;
				double backZ = tz - target.getLookAngle().z * 2;
				spawnKnight(sl, baran, target, backX, ty, backZ, baranFloor, baranOwner);
				sl.sendParticles(ParticleTypes.PORTAL, backX, ty + 1, backZ, 20, 0.5, 1.0, 0.5, 0.3);
				sl.playSound(null, BlockPos.containing(backX, ty, backZ),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.enderman.teleport")),
						SoundSource.HOSTILE, 1.0f, 0.7f);
			}
		}

		if (MF >= 55) {
			resetToIdle(baran);
		}
	}

	private static void spawnKnight(ServerLevel sl, BaranEntity baran, LivingEntity target,
			double sx, double sy, double sz, int floor, String owner) {
		DemonKnightEntity knight = SololevelingModEntities.DEMON_KNIGHT.get()
				.spawn(sl, BlockPos.containing(sx, sy, sz), MobSpawnType.SPAWNER);
		if (knight != null) {
			knight.randomizeVariant();
			knight.getPersistentData().putDouble("dkc_floor_number", floor);
			knight.getPersistentData().putString("dkc_spawned_by", owner);
			knight.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.GLOWING, 100, 0, false, false));
			knight.setTarget(target);
		}
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
