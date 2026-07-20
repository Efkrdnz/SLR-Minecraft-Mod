package net.solocraft.procedures;

import net.solocraft.util.MageSpellProgression;

import net.minecraft.world.entity.Entity;

public class PowerAppendMageProcedure {
	public static void execute(Entity entity) {
		MageSpellProgression.grantEvaluationSpells(entity);
	}
}
