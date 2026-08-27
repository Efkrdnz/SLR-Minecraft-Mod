package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Shadow Feint to any hunter, regardless of class or style. */
public final class RunestoneShadowFeintRCProcedure {
	private RunestoneShadowFeintRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Shadow Feint");
	}
}