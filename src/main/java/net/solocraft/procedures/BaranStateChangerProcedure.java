package net.solocraft.procedures;

import net.solocraft.entity.BaranEntity;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Selects Baran's next attack based on distance to target and phase.
 *
 * Phase 1 attack pool (rand 1-10):
 *   Close range (≤6):  1-2 = ground_slam, 3-4 = charge, 5-6 = magic_blast,
 *                      7-8 = lightning_storm, 9-10 = summon
 *   Far range (>6):    1-3 = magic_blast, 4-5 = lightning_storm,
 *                      6-7 = summon, 8-9 = charge, 10 = ground_slam
 *
 * Phase 2 adds a +2 weight to lightning_storm (more lightning emphasis).
 */
public class BaranStateChangerProcedure {

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null || !(entity instanceof BaranEntity baran))
			return;

		LivingEntity target = (entity instanceof Mob mob) ? mob.getTarget() : null;
		if (target == null) return;

		boolean phase2 = baran.getPersistentData().getBoolean("baran_phase2");
		double distSq = baran.distanceToSqr(target);
		boolean closeRange = distSq <= 36; // 6 blocks

		int rand = Mth.nextInt(RandomSource.create(), 1, phase2 ? 12 : 10);
		String newState;

		if (closeRange) {
			// Close range: prefer physical attacks but keep ranged options
			if (rand <= 2) {
				newState = "ground_slam";
			} else if (rand <= 4) {
				newState = "charge";
			} else if (rand <= 6) {
				newState = "magic_blast";
			} else if (rand <= 8) {
				newState = "lightning_storm";
			} else {
				newState = "summon";
			}
		} else {
			// Far range: prefer ranged attacks
			if (rand <= 3) {
				newState = "magic_blast";
			} else if (rand <= 6) {
				newState = "lightning_storm";  // phase 2 extends this range by 2 extra (7-8)
			} else if (rand <= 8) {
				newState = "summon";
			} else if (rand <= 10) {
				newState = "charge";
			} else {
				// Phase 2 only (rand 11-12)
				newState = "lightning_storm";
			}
		}

		// Apply movement slow during attack windup (briefly)
		if (!baran.level().isClientSide()) {
			baran.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, false, false));
		}

		baran.setState(newState);
		baran.getPersistentData().putDouble("MF", 0);
	}
}
