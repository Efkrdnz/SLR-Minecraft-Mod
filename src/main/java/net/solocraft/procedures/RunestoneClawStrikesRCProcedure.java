package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Claw Strikes to any hunter, regardless of class or style. */
public final class RunestoneClawStrikesRCProcedure {
	private RunestoneClawStrikesRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Claw Strikes");
	}
}
