package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Teaches Titan's Barrage to any hunter, regardless of class or style. */
public final class RunestoneTitansBarrageRCProcedure {
	private RunestoneTitansBarrageRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		RunestoneGrantHelper.grant(entity, itemstack, "Titan's Barrage");
	}
}
