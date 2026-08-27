package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/** Grants the next missing Healer ability in a defined mastery order. */
public final class MasterylvlupHealerProcedure {
	private MasterylvlupHealerProcedure() {
	}

	public static void execute(Entity entity) {
		ClassProgressionHelper.grantNextMasterySkill(entity);
	}
}
