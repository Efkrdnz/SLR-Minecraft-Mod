package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * Baran tears open a dark rift and summons Demon Knights to fight for him.
 *
 * Timeline (MF ticks):
 *   1   – dark rift particles + summoning sound
 *   20  – spawn 2 Demon Knights (phase 2: 3 Knights) near Baran
 *   35  – spawn 1 more Demon Knight behind the target (ambush!) [phase 2 only]
 *   ≥55 – reset to idle
 *
 * Summoned knights inherit the DKC owner/floor but use the separate boss_add
 * role, so they never count toward a floor objective and can be capped/cleaned.
 */
public class BaranSummonProcedure {
	public static final String BOSS_ADD_ROLE = "boss_add";
	private static final int MAX_ACTIVE_BOSS_ADDS = 8;

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
		if ((baranFloor <= 0 || baranOwner.isBlank()) && target instanceof ServerPlayer player
				&& DkcFloorRegistry.isDkc(player.level())
				&& DKCFloorDetectorProcedure.getCurrentFloor(player) == 20) {
			baranFloor = 20;
			baranOwner = player.getStringUUID();
			baran.getPersistentData().putDouble("dkc_floor_number", baranFloor);
			baran.getPersistentData().putString("dkc_spawned_by", baranOwner);
		}

		if (MF == 1) {
			baran.animationprocedure = "attack";
			// Dark rift visuals at Baran's position
			if (world instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.PORTAL, x, y + 1, z, 60, 1.5, 1.5, 1.5, 0.5);
				sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 1, z, 20, 0.5, 0.5, 0.5, 0.02);
				sl.playSound(null, BlockPos.containing(x, y, z),
						ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.wither.spawn")),
						SoundSource.HOSTILE, 1.0f, 1.5f);
			}
		}

		if (MF == 20) {
			if (world instanceof ServerLevel sl) {
				int available = Math.max(0, MAX_ACTIVE_BOSS_ADDS
						- activeBossAdds(sl, baran, baranFloor, baranOwner));
				int count = Math.min(phase2 ? 3 : 2, available);
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
				if (activeBossAdds(sl, baran, baranFloor, baranOwner) >= MAX_ACTIVE_BOSS_ADDS)
					return;
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
			knight.getPersistentData().putString(DKCDemonSpawnerProcedure.ROLE_TAG, BOSS_ADD_ROLE);
			knight.setTarget(target);
		}
	}

	private static int activeBossAdds(ServerLevel level, BaranEntity baran, int floor, String owner) {
		AABB area = baran.getBoundingBox().inflate(128.0D, 96.0D, 128.0D);
		return level.getEntitiesOfClass(DemonKnightEntity.class, area, knight -> {
			var tag = knight.getPersistentData();
			return BOSS_ADD_ROLE.equals(tag.getString(DKCDemonSpawnerProcedure.ROLE_TAG))
					&& floor == (int) tag.getDouble("dkc_floor_number")
					&& owner.equals(tag.getString("dkc_spawned_by"));
		}).size();
	}

	public static void discardBossAdds(ServerLevel level, ServerPlayer player, int floor) {
		String owner = player.getStringUUID();
		level.getEntitiesOfClass(DemonKnightEntity.class, DkcFloorBuilder.combatBounds(player, floor), knight -> {
			var tag = knight.getPersistentData();
			return BOSS_ADD_ROLE.equals(tag.getString(DKCDemonSpawnerProcedure.ROLE_TAG))
					&& floor == (int) tag.getDouble("dkc_floor_number")
					&& owner.equals(tag.getString("dkc_spawned_by"));
		}).forEach(Entity::discard);
	}

	private static void resetToIdle(BaranEntity baran) {
		baran.setState("idle");
		baran.getPersistentData().putDouble("MF", 0);
	}
}
