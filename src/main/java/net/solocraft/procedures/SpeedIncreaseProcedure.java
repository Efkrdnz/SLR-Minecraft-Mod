package net.solocraft.procedures;

import net.solocraft.util.StatInvestmentHelper;

import net.minecraft.world.entity.Entity;

/** Spends skill points on AGILITY at the shared SkillPointRules rate. */
public class SpeedIncreaseProcedure {
	public static void execute(Entity entity) {
		StatInvestmentHelper.invest(entity, StatInvestmentHelper.Stat.AGILITY);
	}
}
