package net.solocraft.procedures;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

public class HammerLivingEntityIsHitWithToolProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		world.levelEvent(2001, BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ()), Block.getId((world.getBlockState(BlockPos.containing(entity.getX(), entity.getY() - 1, entity.getZ())))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 0), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + 0, entity.getY() - 1, entity.getZ() + 1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + -1, entity.getY() - 1, entity.getZ() + -1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + 0, entity.getY() - 1, entity.getZ() + -1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + -1, entity.getY() - 1, entity.getZ() + 0), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + -1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
		world.levelEvent(2001, BlockPos.containing(entity.getX() + -1, entity.getY() - 1, entity.getZ() + 1), Block.getId((world.getBlockState(BlockPos.containing(entity.getX() + 1, entity.getY() - 1, entity.getZ() + 1)))));
	}
}
