package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Legacy hook retained for generated references. Lowering a limit never strips earned vessels. */
public class ResetAfterReadjustingMonarchLimitProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		// VesselClaimSavedData enforces the current limit only for new assignments.
	}
}
