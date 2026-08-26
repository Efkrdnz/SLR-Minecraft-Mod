package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/**
 * Reconciles E-through-S Healer entitlements without removing learned skills.
 *
 * <p>The procedure this replaces checked for {@code Blessing Mark} and then
 * appended {@code Sword of Light}, so an S-rank Healer was awarded a Fighter
 * sword skill and never received its capstone.</p>
 */
public final class PowerAppendHealerProcedure {
	private PowerAppendHealerProcedure() {
	}

	public static void execute(Entity entity) {
		ClassProgressionHelper.reconcileRankEntitlements(entity);
	}
}
