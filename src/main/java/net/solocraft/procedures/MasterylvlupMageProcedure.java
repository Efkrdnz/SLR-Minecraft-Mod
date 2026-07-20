package net.solocraft.procedures;

import net.solocraft.util.MageSpellProgression;

import net.minecraft.world.entity.Entity;

public class MasterylvlupMageProcedure {
	public static void execute(Entity entity) {
		MageSpellProgression.grantMasterySkill(entity);
	}
}
