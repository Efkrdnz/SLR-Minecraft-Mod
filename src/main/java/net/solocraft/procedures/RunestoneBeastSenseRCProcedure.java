package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Beast Sense to any hunter, regardless of class or style. */
public final class RunestoneBeastSenseRCProcedure {
	private RunestoneBeastSenseRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Beast Sense");
	}
}
