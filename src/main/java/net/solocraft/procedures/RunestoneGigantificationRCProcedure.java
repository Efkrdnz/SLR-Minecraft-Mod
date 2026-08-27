package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Gigantification to any hunter, regardless of class or style. */
public final class RunestoneGigantificationRCProcedure {
	private RunestoneGigantificationRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Gigantification");
	}
}
