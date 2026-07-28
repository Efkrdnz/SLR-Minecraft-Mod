package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/**
 * Compatibility entry point retained for generated mob-effect wiring.
 *
 * <p>Physical Buff is now a derived temporary Strength bonus. It must never
 * mutate the permanent Strength field when an effect is applied or refreshed.</p>
 */
public class PhysicalBuffEffectStartedappliedProcedure {
	public static void execute(Entity entity) {
		// Resolved by TemporaryStatBonusManager from the active effect.
	}
}
