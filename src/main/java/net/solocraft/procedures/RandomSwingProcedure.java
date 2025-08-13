package net.solocraft.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class RandomSwingProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		rand = Mth.nextInt(RandomSource.create(), 1, 3);
		if (rand == 1) {
			Swing1Procedure.execute(world, x, y, z, entity);
		}
		if (rand == 2) {
			Swing2Procedure.execute(world, x, y, z, entity);
		}
		if (rand == 3) {
			Swing3Procedure.execute(world, x, y, z, entity);
		}
	}
}
