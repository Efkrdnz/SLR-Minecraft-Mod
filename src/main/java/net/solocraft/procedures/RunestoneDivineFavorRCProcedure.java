package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Divine Favor to any hunter, regardless of class or style. */
public final class RunestoneDivineFavorRCProcedure {
	private RunestoneDivineFavorRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Divine Favor");
	}
}
