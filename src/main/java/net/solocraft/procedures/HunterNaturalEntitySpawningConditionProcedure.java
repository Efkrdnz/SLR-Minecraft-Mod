package net.solocraft.procedures;

import net.solocraft.init.SololevelingModBlocks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class HunterNaturalEntitySpawningConditionProcedure {
	public static boolean execute(LevelAccessor world, double x, double y, double z) {
		if (!((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.BEDROCK) && !((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == SololevelingModBlocks.UNBREAKABLE_DEEPSLATE.get())) {
			return true;
		}
		return false;
	}
}
