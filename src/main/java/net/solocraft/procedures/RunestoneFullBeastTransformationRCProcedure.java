package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Full Beast Transformation to any hunter, regardless of class or style. */
public final class RunestoneFullBeastTransformationRCProcedure {
	private RunestoneFullBeastTransformationRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Full Beast Transformation");
	}
}
