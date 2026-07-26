package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcQuestProgressTracker;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.util.DkcTargetHighlightManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Lightweight per-player DKC runtime; all recovery scans are instance-bounded. */
public class DKCPlayerTickProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (!(entity instanceof ServerPlayer player) || world == null)
			return;
		DkcFloorBuilder.tickPlayer(player);
		int floor = DkcSpatialLayout.floor(player);
		if (floor <= 0 || player.server == null
				|| !DkcRunSavedData.get(player.server).isGenerated(player, floor))
			return;
		DkcQuestProgressTracker.tick(player);
		DKCFloorQuestStarterProcedure.execute(world, player);
		DKCDemonSpawnerProcedure.checkDelayedSpawn(world, player);
		DkcRadiruManager.tick(player);
		DkcTargetHighlightManager.sync(player, floor);
	}
}
