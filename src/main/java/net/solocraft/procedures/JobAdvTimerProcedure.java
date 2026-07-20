package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Retained for generated call compatibility; completion is now kill-based. */
public class JobAdvTimerProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		// JobChangeQuestManager owns the persisted advancement state and recovery.
	}
}
