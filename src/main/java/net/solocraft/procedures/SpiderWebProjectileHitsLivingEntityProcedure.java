package net.solocraft.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;

public class SpiderWebProjectileHitsLivingEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double sy = 0;
		double sz = 0;
		double sx = 0;
		double rand = 0;
		sy = Mth.nextInt(RandomSource.create(), 1, 2);
		if (rand == 2) {
			sx = -1;
			for (int index0 = 0; index0 < 2; index0++) {
				sy = -1;
				for (int index1 = 0; index1 < 2; index1++) {
					sz = -1;
					for (int index2 = 0; index2 < 2; index2++) {
						if ((world.getBlockState(BlockPos.containing(x + sx, y + sy, z + sz))).getBlock() == Blocks.AIR) {
							world.setBlock(BlockPos.containing(x + sx, y + sy, z + sz), Blocks.COBWEB.defaultBlockState(), 3);
						}
						sz = sz + 1;
					}
					sy = sy + 1;
				}
				sx = sx + 1;
			}
		}
	}
}
