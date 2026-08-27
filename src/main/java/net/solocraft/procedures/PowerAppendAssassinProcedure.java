package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/** Reconciles E-through-S Assassin entitlements without removing learned skills. */
public final class PowerAppendAssassinProcedure {
	private PowerAppendAssassinProcedure() {
	}

	public static void execute(Entity entity) {
		ClassProgressionHelper.reconcileRankEntitlements(entity);
	}
}
