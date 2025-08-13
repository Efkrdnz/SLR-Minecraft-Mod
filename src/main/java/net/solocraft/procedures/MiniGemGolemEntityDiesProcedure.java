package net.solocraft.procedures;

import net.solocraft.init.SololevelingModBlocks;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public class MiniGemGolemEntityDiesProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world instanceof ServerLevel _level)
			FallingBlockEntity.fall(_level, BlockPos.containing(x, y, z), SololevelingModBlocks.GOLEM_DROP_BLOCK_GEM.get().defaultBlockState());
	}
}
