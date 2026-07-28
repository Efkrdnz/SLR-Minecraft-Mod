package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/**
 * Compatibility entry point retained for generated mob-effect wiring.
 *
 * <p>Haste is now a derived temporary Agility bonus. It must never mutate the
 * permanent Speed field when an effect is applied or refreshed.</p>
 */
public class HasteBuffEffectStartedappliedProcedure {
	public static void execute(Entity entity) {
		// Resolved by TemporaryStatBonusManager from the active effect.
	}
}
