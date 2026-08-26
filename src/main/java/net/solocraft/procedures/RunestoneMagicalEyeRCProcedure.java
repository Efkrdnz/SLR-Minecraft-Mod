package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Magical Eye to any hunter, regardless of class or style. */
public final class RunestoneMagicalEyeRCProcedure {
	private RunestoneMagicalEyeRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Magical Eye");
	}
}
