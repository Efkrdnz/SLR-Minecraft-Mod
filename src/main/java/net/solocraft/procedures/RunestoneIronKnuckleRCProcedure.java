package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Iron Knuckle to any hunter, regardless of class or style. */
public final class RunestoneIronKnuckleRCProcedure {
	private RunestoneIronKnuckleRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Iron Knuckle");
	}
}
