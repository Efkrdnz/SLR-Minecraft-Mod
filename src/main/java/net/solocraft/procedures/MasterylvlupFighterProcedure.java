package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

/** Grants the next missing Fighter ability in a defined mastery order. */
public final class MasterylvlupFighterProcedure {
	private MasterylvlupFighterProcedure() {
	}

	public static void execute(Entity entity) {
		ClassProgressionHelper.grantNextMasterySkill(entity);
	}
}
