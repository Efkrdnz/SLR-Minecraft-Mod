package net.solocraft.procedures;

import net.solocraft.util.StatInvestmentHelper;

import net.minecraft.world.entity.Entity;

/** Spends skill points on STRENGTH at the shared SkillPointRules rate. */
public class StrengthIncreaseProcedure {
	public static void execute(Entity entity) {
		StatInvestmentHelper.invest(entity, StatInvestmentHelper.Stat.STRENGTH);
	}
}
