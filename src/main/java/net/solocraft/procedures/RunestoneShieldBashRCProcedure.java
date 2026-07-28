package net.solocraft.procedures;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public final class RunestoneShieldBashRCProcedure {
	private RunestoneShieldBashRCProcedure() {
	}

	public static void execute(Entity entity, ItemStack itemstack) {
		TankerProgressionHelper.learnFromRunestone(
				entity, itemstack, TankerProgressionHelper.SHIELD_BASH);
	}
}
