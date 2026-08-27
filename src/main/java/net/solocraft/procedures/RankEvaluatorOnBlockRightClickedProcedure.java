package net.solocraft.procedures;

import net.solocraft.util.HunterEvaluationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Compatibility entry point used by both evaluator block variants. */
public final class RankEvaluatorOnBlockRightClickedProcedure {
	private RankEvaluatorOnBlockRightClickedProcedure() {
	}

	public static void execute(LevelAccessor world, double x, double y,
			double z, Entity entity) {
		if (entity instanceof ServerPlayer player)
			HunterEvaluationManager.openEvaluator(player,
					BlockPos.containing(x, y, z));
	}
}
