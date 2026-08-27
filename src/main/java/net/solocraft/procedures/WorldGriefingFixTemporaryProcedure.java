package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;

/** Retained as a no-op binary/source compatibility shim for older generated calls. */
@Deprecated
public class WorldGriefingFixTemporaryProcedure {
	public static void execute(LevelAccessor world) {
		// The legacy rule used to be forced true on every server start. Ability
		// destruction is now explicitly opt-in and must never be changed here.
	}
}
