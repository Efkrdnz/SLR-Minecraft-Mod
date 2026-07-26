package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcSpatialLayout;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Compatibility entry point retained for older generated call sites. */
public class EntryPermitInteractionCheckProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		int floor = DkcSpatialLayout.floor(player);
		if (floor > 0 && floor < DkcFloorRegistry.LAST_FLOOR
				&& DkcRunSavedData.get(player.server).isTransitionArmed(player, floor))
			DkcFloorBuilder.prepareFloor(player, floor + 1);
	}
}
