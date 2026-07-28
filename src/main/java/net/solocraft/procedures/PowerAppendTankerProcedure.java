package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/** Reconciles E-through-S Tanker entitlements without removing learned skills. */
public final class PowerAppendTankerProcedure {
	private PowerAppendTankerProcedure() {
	}

	public static void execute(Entity entity) {
		TankerProgressionHelper.reconcileRankEntitlements(entity);
	}
}
