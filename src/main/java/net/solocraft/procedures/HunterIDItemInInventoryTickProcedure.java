package net.solocraft.procedures;

import net.solocraft.item.HunterIDItem;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Legacy generated hook retained for old item definitions. */
public class HunterIDItemInInventoryTickProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		HunterIDItem.refreshStack(entity, itemstack);
	}
}
