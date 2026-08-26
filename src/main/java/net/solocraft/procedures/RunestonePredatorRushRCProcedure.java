package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Predator Rush to any hunter, regardless of class or style. */
public final class RunestonePredatorRushRCProcedure {
	private RunestonePredatorRushRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Predator Rush");
	}
}
