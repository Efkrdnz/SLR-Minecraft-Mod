package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Heavy Blow to any hunter, regardless of class or style. */
public final class RunestoneHeavyBlowRCProcedure {
	private RunestoneHeavyBlowRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Heavy Blow");
	}
}
