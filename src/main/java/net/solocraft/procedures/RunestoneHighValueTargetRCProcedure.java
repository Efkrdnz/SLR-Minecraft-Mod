package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

public class RunestoneHighValueTargetRCProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		RangerCombatManager.learnFromRunestone(entity, itemstack,
				RangerCombatManager.HIGH_VALUE_TARGET);
	}
}
