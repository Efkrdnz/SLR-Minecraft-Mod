package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Purifying Wave to any hunter, regardless of class or style. */
public final class RunestonePurifyingWaveRCProcedure {
	private RunestonePurifyingWaveRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Purifying Wave");
	}
}
