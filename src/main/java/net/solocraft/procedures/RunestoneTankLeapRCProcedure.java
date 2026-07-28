package net.solocraft.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public final class RunestoneTankLeapRCProcedure {
	private RunestoneTankLeapRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		TankerProgressionHelper.learnFromRunestone(
				entity, itemstack, TankerProgressionHelper.TANK_LEAP);
	}
}
