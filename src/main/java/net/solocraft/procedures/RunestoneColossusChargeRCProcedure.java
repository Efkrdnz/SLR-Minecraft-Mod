package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Colossus Charge to any hunter, regardless of class or style. */
public final class RunestoneColossusChargeRCProcedure {
	private RunestoneColossusChargeRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Colossus Charge");
	}
}
