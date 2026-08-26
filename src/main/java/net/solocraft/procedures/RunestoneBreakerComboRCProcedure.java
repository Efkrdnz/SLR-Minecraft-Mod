package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Breaker Combo to any hunter, regardless of class or style. */
public final class RunestoneBreakerComboRCProcedure {
	private RunestoneBreakerComboRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Breaker Combo");
	}
}
