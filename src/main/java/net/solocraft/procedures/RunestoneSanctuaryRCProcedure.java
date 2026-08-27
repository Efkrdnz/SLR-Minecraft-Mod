package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Sanctuary to any hunter, regardless of class or style. */
public final class RunestoneSanctuaryRCProcedure {
	private RunestoneSanctuaryRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Sanctuary");
	}
}
