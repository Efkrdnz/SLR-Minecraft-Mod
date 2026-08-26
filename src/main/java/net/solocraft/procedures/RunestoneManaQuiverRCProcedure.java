package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class RunestoneManaQuiverRCProcedure {
	public static void execute(Entity entity, ItemStack itemstack) {
		RangerCombatManager.learnManaQuiverFromRunestone(entity, itemstack);
	}
}
