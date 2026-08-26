package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Vitality Surge to any hunter, regardless of class or style. */
public final class RunestoneVitalitySurgeRCProcedure {
	private RunestoneVitalitySurgeRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Vitality Surge");
	}
}
