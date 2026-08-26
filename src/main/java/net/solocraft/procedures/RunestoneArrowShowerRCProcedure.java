package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Arrow Shower to any hunter, regardless of class or style. */
public final class RunestoneArrowShowerRCProcedure {
	private RunestoneArrowShowerRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Arrow Shower");
	}
}
