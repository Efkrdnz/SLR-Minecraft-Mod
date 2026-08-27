package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Radiant Execution to any hunter, regardless of class or style. */
public final class RunestoneRadiantExecutionRCProcedure {
	private RunestoneRadiantExecutionRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Radiant Execution");
	}
}
