package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public class RunestoneBackstepRCProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		RangerCombatManager.learnFromRunestone(entity, itemstack, RangerCombatManager.BACK_STEP);
	}
}
