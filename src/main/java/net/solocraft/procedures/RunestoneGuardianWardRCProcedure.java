package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Guardian Ward to any hunter, regardless of class or style. */
public final class RunestoneGuardianWardRCProcedure {
	private RunestoneGuardianWardRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Guardian Ward");
	}
}
