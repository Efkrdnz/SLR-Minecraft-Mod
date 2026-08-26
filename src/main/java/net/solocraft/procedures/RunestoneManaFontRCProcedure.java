package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Mana Font to any hunter, regardless of class or style. */
public final class RunestoneManaFontRCProcedure {
	private RunestoneManaFontRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Mana Font");
	}
}
