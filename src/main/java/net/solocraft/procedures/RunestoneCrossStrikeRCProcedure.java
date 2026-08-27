package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Cross Strike to any hunter, regardless of class or style. */
public final class RunestoneCrossStrikeRCProcedure {
	private RunestoneCrossStrikeRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Cross Strike");
	}
}
