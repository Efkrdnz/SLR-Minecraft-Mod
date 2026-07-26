package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcSpatialLayout;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/**
 * Legacy generated entry point. Live DKC generation now uses the staged modular
 * builder; bossType is intentionally ignored because bosses are runtime-owned.
 */
public class FloorCreateNewProcedure {
	public static void execute(LevelAccessor world, Entity entity, String bossType) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		int floor = DkcSpatialLayout.floor(player);
		if (floor == 0)
			floor = Math.max(1, Math.min(DkcFloorRegistry.LAST_FLOOR,
					(int) player.getPersistentData().getDouble("dkc_current_floor")));
		DkcRunSavedData.get(player.server).unlockFloor(player, floor);
		DkcFloorBuilder.prepareFloor(player, floor);
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(world, entity, "normal");
	}
}
