package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/** Reconciles E-through-S Fighter entitlements without removing learned skills. */
public final class PowerAppendFighterProcedure {
	private PowerAppendFighterProcedure() {
	}

	public static void execute(Entity entity) {
		ClassProgressionHelper.reconcileRankEntitlements(entity);
	}
}
