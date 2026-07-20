package net.solocraft.world.dimension.rift;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/** Creates and targets the temporary central arrival dais. */
public final class DimensionalRiftEntry {
	public static final BlockPos PLATFORM_CENTER = new BlockPos(0, 86, 0);
	public static final BlockPos PLAYER_ARRIVAL = PLATFORM_CENTER.above();

	private DimensionalRiftEntry() {
	}

	public static void ensurePlatform(ServerLevel level) {
		if (level.getBlockState(PLATFORM_CENTER).is(Blocks.CHISELED_POLISHED_BLACKSTONE))
			return;
		int radius = 7;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				int squared = dx * dx + dz * dz;
				if (squared > radius * radius)
					continue;
				BlockPos floor = PLATFORM_CENTER.offset(dx, 0, dz);
				boolean rim = squared >= (radius - 1) * (radius - 1);
				level.setBlock(floor, rim ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
						: Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), 3);
				for (int dy = 1; dy <= 5; dy++)
					level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
			}
		}
		level.setBlock(PLATFORM_CENTER, Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(), 3);
		for (int[] offset : new int[][] {{4, 0}, {-4, 0}, {0, 4}, {0, -4}})
			level.setBlock(PLATFORM_CENTER.offset(offset[0], 0, offset[1]), Blocks.SEA_LANTERN.defaultBlockState(), 3);
	}

	public static void teleportToCenter(ServerPlayer player, ServerLevel level) {
		ensurePlatform(level);
		player.teleportTo(level, PLAYER_ARRIVAL.getX() + 0.5D, PLAYER_ARRIVAL.getY(),
				PLAYER_ARRIVAL.getZ() + 0.5D, 0.0F, 0.0F);
	}
}
