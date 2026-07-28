package net.solocraft.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public final class RunestoneTauntRCProcedure {
	private RunestoneTauntRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		TankerProgressionHelper.learnFromRunestone(
				entity, itemstack, TankerProgressionHelper.TAUNT);
	}
}
