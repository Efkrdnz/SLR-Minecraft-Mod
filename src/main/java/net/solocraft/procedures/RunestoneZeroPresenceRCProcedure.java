package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Zero Presence to any hunter, regardless of class or style. */
public final class RunestoneZeroPresenceRCProcedure {
	private RunestoneZeroPresenceRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Zero Presence");
	}
}