package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Reconciles deterministic Ranger rank skills. Mana Quiver is deliberately
 * granted before and independently of every Hunter-rank threshold.
 */
public final class PowerAppendRangerProcedure {
	private PowerAppendRangerProcedure() {
	}

	public static void execute(Entity entity) {
		if (entity instanceof ServerPlayer player)
			RangerCombatManager.reconcileRanger(player);
	}
}
