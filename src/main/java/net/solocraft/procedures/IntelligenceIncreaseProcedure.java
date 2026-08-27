package net.solocraft.procedures;

import net.solocraft.util.StatInvestmentHelper;

import net.minecraft.world.entity.Entity;

/** Spends skill points on INTELLIGENCE at the shared SkillPointRules rate. */
public class IntelligenceIncreaseProcedure {
	public static void execute(Entity entity) {
		StatInvestmentHelper.invest(entity, StatInvestmentHelper.Stat.INTELLIGENCE);
	}
}
