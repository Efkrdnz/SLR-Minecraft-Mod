package net.solocraft.procedures;

import net.solocraft.init.SololevelingModBlocks;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class CellDoorOpenOnBlockRightClickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		world.setBlock(BlockPos.containing(x, y, z), SololevelingModBlocks.CELL_DOOR_CLOSED.get().defaultBlockState(), 3);
	}
}
