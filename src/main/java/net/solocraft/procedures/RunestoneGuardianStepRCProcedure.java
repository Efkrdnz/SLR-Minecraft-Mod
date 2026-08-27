package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Guardian Step to any hunter, regardless of class or style. */
public final class RunestoneGuardianStepRCProcedure {
	private RunestoneGuardianStepRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Guardian Step");
	}
}
