package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Meteor Fist to any hunter, regardless of class or style. */
public final class RunestoneMeteorFistRCProcedure {
	private RunestoneMeteorFistRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Meteor Fist");
	}
}
