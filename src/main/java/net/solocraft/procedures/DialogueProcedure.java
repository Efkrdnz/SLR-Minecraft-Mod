package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/**
 * Retired Secretary interaction hook. The NPC remains decorative and cannot
 * start, advance, or reevaluate a player.
 */
@Deprecated(forRemoval = false)
public final class DialogueProcedure {
	private DialogueProcedure() {
	}

	public static void execute(LevelAccessor world, double x, double y,
			double z, Entity sourceEntity) {
		// Intentionally empty.
	}
}
