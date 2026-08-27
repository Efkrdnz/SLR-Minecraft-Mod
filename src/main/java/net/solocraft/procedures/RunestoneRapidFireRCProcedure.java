package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Rapid Fire to any hunter, regardless of class or style. */
public final class RunestoneRapidFireRCProcedure {
	private RunestoneRapidFireRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Rapid Fire");
	}
}
